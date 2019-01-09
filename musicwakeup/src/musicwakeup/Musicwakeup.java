package musicwakeup;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import hu.qgears.commons.UtilFile;
import hu.qgears.commons.UtilString;
import joptsimple.annot.AnnotatedClass;
import musicwakeup.ShutterControl.Commands;

/**
 * Wake up on music program.
 * @author rizsi
 *
 */
public class Musicwakeup extends JFrame {
	private static final long serialVersionUID = 1L;
	private SimpleDateFormat df=new SimpleDateFormat("yyyy. M. dd. HH:mm");
	public WakeupArgs clargs=new WakeupArgs();
	public class Args
	{
		public File playlist;
		public long wakeupTime;
		public String wakeupString="9:00";
		public String volumeControl="50,500:100";
		public boolean disableArduinoShutterControl;
		public String serialize() {
			return "--playlist\n"+(playlist==null?"":playlist.getAbsolutePath())+"\n--wakeupTime\n"+wakeupTime+"\n--volumeControl\n"+volumeControl+"\n--wakeupString\n"+wakeupString+"";
		}
	}
	abstract class DocListener implements DocumentListener
	{

		@Override
		public void insertUpdate(DocumentEvent e) {
			upd();
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			upd();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			upd();
		}
		protected abstract void upd();
	}
	class WakeupProcess
	{
		private Playback p;
		private VolumeControl vc;
		public void execute(VolumeControl vc, File playlist)
		{
			this.vc=vc;
			Runnable atend=new Runnable() {
				@Override
				public void run() {
					updateUI();
				}
			};
			vc.start(atend);
			try {
				p=play(playlist, atend);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(shutterControl!=null)
			{
				shutterControl.iterateCommand(Commands.up, 5, 5000);
			}
		}
		
		public void stop()
		{
			try {
				vc.stop();
				p.stop();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Runnable atend;
		public Playback play(File playlist, Runnable atend) throws IOException
		{
			return new Playback(atend).play(playlist);
		}

		protected void playProcessFinished() {
			stopVolumeControl();
			updateUI();
		}

		public void stopVolumeControl() {
			vc.stop();
		}

		public boolean isFinished() {
			return p!=null&&p.isFinished()&&vc.isFinished();
		}
		
	}
	private WakeupProcess w;
	public void wakeup(boolean force)
	{
		if(force)
		{
			stopPlay();
		}
		if(w==null)
		{
			w=new WakeupProcess();
			VolumeControl vc=new VolumeControl();
			try {
				vc.parse(volumeControl.getText());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			w.execute(vc, args.playlist);
		}
		updateUI();
	}
	private void updateUI()
	{
		if(!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					updateUI();
				}
			});
		}
		if(w!=null && w.isFinished())
		{
			w=null;
		}
		stop.setEnabled(w!=null);
		stopVolume.setEnabled(w!=null&&!w.vc.isFinished());
		playList.setEnabled(w==null);
	}
	public void stopPlay()
	{
		if(w!=null)
		{
			w.stop();
			w=null;
		}
		updateUI();
	}
	public void stopVolumeControl()
	{
		if(w!=null)
		{
			w.stopVolumeControl();
		}
	}
	private JLabel status;
	private JButton stop;
	JButton playList;
	JButton stopVolume;
	JTextField volumeControl;
	Args args = new Args();
	JButton playlist;
	public Musicwakeup() {
		try {
			AnnotatedClass cl = new AnnotatedClass();
			cl.parseAnnotations(args);
			String[] argspieces=UtilString.split(UtilFile.loadAsString(clargs.wakeupTFile), " \r\n").toArray(new String[]{});
			cl.parseArgs(argspieces);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		setTitle("Wakeup program");
		setSize(1024, 768);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		getContentPane().setLayout(new FlowLayout(FlowLayout.LEFT));
		JButton wa=new JButton("Shutdown and start wakeup");
		JButton setTime=new JButton("Save settings");
		setTime.setEnabled(false);
		setTime.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				shutdownToWakeup(false);
			}
		});
		add(setTime);
		wa.setEnabled(false);
		add(wa);
		wa.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				shutdownToWakeup(true);
			}
		});
		add(new JLabel("Wake up time (HH:MM):"));
		JTextField time=new JTextField(20);
		time.setText(args.wakeupString);
		time.getDocument().addDocumentListener(new DocListener() {
			
			@Override
			protected void upd() {
				updateButton(setTime, wa, time);
			}
		});
		add(time);
		updateButton(setTime, wa, time);
		add(new JLabel("Volume control: "));
		volumeControl=new JTextField(40);
		volumeControl.setText(args.volumeControl);
		volumeControl.setToolTipText("'50,500:100' means that we start at 50% and in 500 seconds increase to 100% gradually");
		add(volumeControl);
		volumeControl.getDocument().addDocumentListener(new DocListener() {
			
			@Override
			protected void upd() {
				volumeControlUpdated();
			}
		});
		volumeControlUpdated();
		playList=new JButton("Play wakeup playlist");
		playList.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				wakeup(false);
			}
		});
		add(playList);
		{
			stop=new JButton("Stop music player");
			stop.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					stopPlay();
				}
			});
			add(stop);
		}
		{
			stopVolume=new JButton("Stop volume control");
			stopVolume.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					stopVolumeControl();
				}
			});
			add(stopVolume);
		}
		{
			playlist=new JButton("Playlist: "+args.playlist);
			add(playlist);
			playlist.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					File f=args.playlist;
					JFileChooser jfc;
					if(f.exists())
					{
						if(f.isDirectory())
						{
							jfc=new JFileChooser(f);
						}else
						{
							jfc=new JFileChooser(f.getParentFile());
						}
					}else
					{
						jfc=new JFileChooser();
					}
					jfc.showOpenDialog(Musicwakeup.this);
					File sel=jfc.getSelectedFile();
					args.playlist=sel;
					playlist.setText("Playlist: "+args.playlist);
				}
			});
		}
		status=new JLabel();
		add(status);
		add(createShutterButton(ShutterControl.Commands.up));
		add(createShutterButton(ShutterControl.Commands.stop));
		add(createShutterButton(ShutterControl.Commands.down));
		updateUI();
		Timer timer = new Timer(5000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				checkStartWakeup();
			}
		});
		timer.start();
		if(!args.disableArduinoShutterControl)
		{
			shutterControl=new ShutterControl();
			shutterControl.start();
		}
	}
	private JButton createShutterButton(Commands command) {
		JButton b=new JButton("Shutter "+command);
		b.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(shutterControl!=null)
				{
					try {
						shutterControl.commandAsync(command);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		return b;
	}
	private ShutterControl shutterControl;
	protected void volumeControlUpdated() {
		try {
			args.volumeControl=volumeControl.getText();
			new VolumeControl().parse(volumeControl.getText());
			volumeControl.setBackground(Color.white);
		} catch (Exception e) {
			volumeControl.setBackground(Color.red);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	protected void checkStartWakeup() {
		if(w==null)
		{
			long diff=args.wakeupTime-System.currentTimeMillis();
			Calendar c=Calendar.getInstance();
			c.setTimeInMillis(args.wakeupTime);
			status.setText("Wakeup time: "+df.format(c.getTime())+" Time to wakeup in seconds: "+(diff/1000));
			if(diff<0&&Math.abs(diff)<15*1000*60)
			{
				wakeup(true);
				args.wakeupTime=0;
			}
		}
	}
	private long wakeupTimeOnGui;
	protected void shutdownToWakeup(boolean shutdown) {
		args.wakeupTime=wakeupTimeOnGui;
		long seconds=(args.wakeupTime-System.currentTimeMillis())/1000;
		System.out.println("Seconds to wake up: "+seconds);
		try {
			UtilFile.saveAsFile(clargs.wakeupTFile, args.serialize());
			if(shutdown)
			{
				ProcessBuilder pb=new ProcessBuilder("sudo", "rtcwake", "-m", "off", "-s", ""+seconds);
				pb.start();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void updateButton(JButton setTime, JButton wa, JTextField time) {
		args.wakeupString=time.getText();
		  wa.setText("Enter valid wakeup time (HH:MM)");
		  setTime.setEnabled(false);
		  wa.setEnabled(false);
		  try {
			List<String> pieces=UtilString.split(time.getText(), ":");
			  if(pieces.size()==2)
			  {
				  int hr=Integer.parseInt(pieces.get(0));
				  int mimute=Integer.parseInt(pieces.get(1));
				  wa.setText(""+hr+":"+mimute);
				  wa.setEnabled(true);
				  setTime.setEnabled(true);
				  Calendar c=Calendar.getInstance();
				  System.out.println(""+c);
				  Calendar newC=Calendar.getInstance();
				  newC.set(Calendar.HOUR_OF_DAY, hr);
				  newC.set(Calendar.MINUTE, mimute);
				  if(newC.before(c))
				  {
					  newC.add(Calendar.DAY_OF_MONTH, 1);
				  }
				  wa.setText("Shutdown: "+df.format(newC.getTime()));
				  wakeupTimeOnGui=newC.getTimeInMillis();
			  }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			Musicwakeup ex = new Musicwakeup();
			ex.setVisible(true);
		});
	}
}
