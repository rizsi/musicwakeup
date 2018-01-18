package musicwakeup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hu.qgears.commons.UtilProcess;
import hu.qgears.commons.UtilString;
import hu.qgears.commons.signal.SignalFuture;
import hu.qgears.commons.signal.Slot;

public class Playback {
	private volatile boolean finished=false;
	private Runnable atend;
	public Playback(Runnable atend) {
		this.atend=atend;
	}

	public void setVolume(int vol)
	{
		new Thread("set vlc volume"){
			@Override
			public void run() {
				for(int i=0;i<5;++i)
				{
					try {
						Thread.sleep(1000);
						Process volume=new ProcessBuilder("pactl", "list", "sink-inputs").start();
						List<String> lines=UtilString.split(UtilProcess.execute(volume), "\r\n");
						int number=-1;
						for(String s: lines)
						{
							if(s.startsWith("Sink Input #"))
							{
								String n=s.substring("Sink Input #".length());
								number=Integer.parseInt(n);
							}
							if(s.contains("application.process.binary")&&s.contains("\"vlc\""))
							{
								System.out.println("set volume of sink-input: "+number+" "+vol+"%");
								new ProcessBuilder("pactl", "set-sink-input-volume", ""+number, ""+vol+"%").start();
							}
						}
					} catch (Exception e) {
						System.err.println("Setting volume: "+e.getMessage());
					}
				}
			}
		}.start();
	}
	private Process p;
	public Playback play(File playlist) {
		try {
			List<String> pieces=new ArrayList<>();
			pieces.add("vlc");
			pieces.add(playlist.getAbsolutePath());
			System.out.println("Command: "+pieces);
			ProcessBuilder pb=new ProcessBuilder(pieces);
			p=pb.start();
			setVolume(100);
			UtilProcess.streamOutputsOfProcess(p);
			UtilProcess.getProcessReturnValueFuture(p).addOnReadyHandler(new Slot<SignalFuture<Integer>>() {
				
				@Override
				public void signal(SignalFuture<Integer> value) {
					playProcessFinished();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
			finished=true;
		}
		return this;
	}

	protected void playProcessFinished() {
		finished=true;
		atend.run();
	}

	public void stop() {
		try {
			p.destroy();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isFinished() {
		return finished;
	}
}
