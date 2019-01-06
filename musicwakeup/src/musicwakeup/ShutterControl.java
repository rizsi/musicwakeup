package musicwakeup;

import java.io.File;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import hu.qgears.commons.signal.SignalFutureWrapper;

public class ShutterControl {
	public static enum Commands
	{
		up('C'),down('A'), stop('B');
		public final char c;
		Commands(char c)
		{
			this.c=c;
		}
	}
	private boolean exit=false;
	public static void main(String[] args) throws Exception {
		ShutterControl sc=new ShutterControl();
		sc.start();
		sc.command(Commands.up);
		Thread.sleep(1000);
		sc.command(Commands.stop);
		Thread.sleep(1000);
		sc.command(Commands.down);
		Thread.sleep(1000);
		sc.command(Commands.stop);
	}
	public void start() {
		new Thread("ShutterControl"){
			@Override
			public void run() {
				ShutterControl.this.run();
			}
		}.start();
	}
	public SignalFutureWrapper<ShutterControl> commandAsync(Commands comm) throws InterruptedException {
		SignalFutureWrapper<ShutterControl> ret=new SignalFutureWrapper<>();
		commands.put(new Runnable() {
			@Override
			public void run() {
				try {
					os.write((byte)comm.c);
					os.flush();
					System.out.println("Shutter command sent: "+comm+" "+comm.c);
					ret.ready(ShutterControl.this, null);
				} catch (Exception e) {
					ret.ready(ShutterControl.this, e);
				}
			}
		});
		return ret;
	}
	public void command(Commands comm) throws InterruptedException, ExecutionException {
		commandAsync(comm).get();
	}
	private OutputStream os;
	private LinkedBlockingQueue<Runnable> commands=new LinkedBlockingQueue<>();
	public void run() {
		while(!exit)
		{
			try {
				for(int i=0;i<10;++i)
				{
					String dev="/dev/ttyACM"+i;
					if(new File(dev).exists())
					{
						System.out.println("Starting Arduino connection on: '"+dev+"'");
						String baud="b9600";
						ProcessBuilder pb=new ProcessBuilder("/usr/bin/socat",
								"-", "file:"+dev+",nonblock,raw,echo=0,"+baud);
						pb.redirectOutput(Redirect.INHERIT);
						pb.redirectError(Redirect.INHERIT);
						Process p=pb.start();
						// Arduino reboots when reconnected with current configuration.
						Thread.sleep(3000);
						System.out.println("Ardino reboot should have finished");
						os=p.getOutputStream();
						try {
							Runnable command;
							while(p.isAlive())
							{
								while((command=commands.poll(1, TimeUnit.SECONDS))!=null)
								{
									command.run();
								}
							}
						} finally
						{
							os=null;
						}
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void iterateCommand(Commands up, int nIterate, long waitMillis) {
		new Thread("Shutter iterate command: "+up){
			@Override
			public void run() {
				for(int i=0;i<nIterate;++i)
				{
					try {
						command(up);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					try {
						Thread.sleep(waitMillis);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
}
