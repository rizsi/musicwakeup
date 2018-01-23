package musicwakeup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hu.qgears.commons.Pair;
import hu.qgears.commons.UtilProcess;
import hu.qgears.commons.UtilString;

public class VolumeControl {
	private int v0;
	List<Pair<Integer, Integer>> sections=new ArrayList<>();
	private volatile boolean finished=false;
	public void parse(String setup)
	{
		List<String> points=UtilString.split(setup, ",");
		v0=Integer.parseInt(points.get(0));
		for(int i=1;i<points.size();++i)
		{
			List<String> pieces=UtilString.split(points.get(i), ":");
			int t=Integer.parseInt(pieces.get(0));
			int v=Integer.parseInt(pieces.get(1));
			sections.add(new Pair<Integer, Integer>(t, v));
		}
	}
	@Override
	public String toString() {
		StringBuilder ret=new StringBuilder();
		ret.append("Initial volume: "+v0);
		for(Pair<Integer, Integer> section: sections)
		{
			ret.append(" in "+section.getA()+"seconds to "+section.getB());
		}
		return super.toString();
	}
	private Thread th;
	private volatile boolean exit=false;
	public void start(Runnable finishedListener)
	{
		setVolume(v0);
		th=new Thread("Volume control")
		{
			@Override
			public void run() {
				int curr=v0;
				for(Pair<Integer, Integer> stage: sections)
				{
					long millis=stage.getA()*1000;
					int tgVol=stage.getB();
					int steps=Math.abs(tgVol-curr);
					int volDir=(int)Math.signum(tgVol-curr);
					try {
						if(steps==0)
						{
							Thread.sleep(millis);
						}else
						{
							long stepMillis=millis/steps;
							for(int i=0;i<steps&&!finished;++i)
							{
								Thread.sleep(stepMillis);
								curr+=volDir;
								setVolume(curr);
							}
						}
					} catch (InterruptedException e) {
					}
					if(exit)
					{
						break;
					}
				}
				finished=true;
				finishedListener.run();
			}
		};
		th.start();
	}
	public void stop()
	{
		exit=true;
		if(th!=null)
		{
			try {
				th.interrupt();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public static void setVolume(int i) {
		try {
			System.out.println("Volume to: "+i);
			{
				ProcessBuilder pb=new ProcessBuilder("pactl", "set-sink-mute", "@DEFAULT_SINK@", "0");
				Process p=pb.start();
				UtilProcess.streamOutputsOfProcess(p);
			}
			{
				ProcessBuilder pb=new ProcessBuilder("pactl", "set-sink-volume", "@DEFAULT_SINK@", ""+i+"%");
				Process p=pb.start();
				UtilProcess.streamOutputsOfProcess(p);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	public boolean isFinished() {
		return finished;
	}

}
