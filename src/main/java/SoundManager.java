import javax.sound.sampled.*;

/**
 * 音效管理器。使用程序合成音效，无需外部音频文件。
 * 所有音效在后台线程播放，不阻塞UI。
 */
public class SoundManager {

	private static boolean enabled = true;

	public static void setEnabled(boolean on) {
		enabled = on;
	}

	public static boolean isEnabled() {
		return enabled;
	}

	/** 落子音效：短促清脆的敲击声 */
	public static void playPlace() {
		if (!enabled) return;
		playAsync(() -> {
			try {
				int sampleRate = 22050;
				int samples = sampleRate / 10; // 0.1秒
				byte[] buf = new byte[samples];
				for (int i = 0; i < samples; i++) {
					double t = (double) i / sampleRate;
					double env = Math.exp(-t * 40); // 快速衰减
					double wave = Math.sin(2 * Math.PI * 800 * t) * 0.6
							+ Math.sin(2 * Math.PI * 1200 * t) * 0.3
							+ Math.sin(2 * Math.PI * 2000 * t) * 0.1;
					buf[i] = (byte) (wave * env * 100);
				}
				playBuffer(buf, sampleRate);
			} catch (Exception ignored) {}
		});
	}

	/** 胜利音效：上行琶音 */
	public static void playVictory() {
		if (!enabled) return;
		playAsync(() -> {
			try {
				int sampleRate = 22050;
				double[] notes = {523.25, 659.25, 783.99, 1046.50}; // C5 E5 G5 C6
				int noteLen = sampleRate / 6; // 每个音符约0.17秒
				int total = noteLen * notes.length;
				byte[] buf = new byte[total];
				for (int n = 0; n < notes.length; n++) {
					for (int i = 0; i < noteLen; i++) {
						int idx = n * noteLen + i;
						double t = (double) i / sampleRate;
						double env = Math.min(1.0, (noteLen - i) / (double) (noteLen / 3));
						env = Math.min(env, i / (double) (sampleRate / 100));
						double wave = Math.sin(2 * Math.PI * notes[n] * t);
						buf[idx] = (byte) (wave * env * 80);
					}
				}
				playBuffer(buf, sampleRate);
			} catch (Exception ignored) {}
		});
	}

	/** 失败音效：下行音阶 */
	public static void playDefeat() {
		if (!enabled) return;
		playAsync(() -> {
			try {
				int sampleRate = 22050;
				double[] notes = {392.00, 349.23, 293.66, 261.63}; // G4 F4 D4 C4
				int noteLen = sampleRate / 4; // 每个音符约0.25秒
				int total = noteLen * notes.length;
				byte[] buf = new byte[total];
				for (int n = 0; n < notes.length; n++) {
					for (int i = 0; i < noteLen; i++) {
						int idx = n * noteLen + i;
						double t = (double) i / sampleRate;
						double env = Math.min(1.0, (noteLen - i) / (double) (noteLen / 2));
						env = Math.min(env, i / (double) (sampleRate / 100));
						double wave = Math.sin(2 * Math.PI * notes[n] * t);
						buf[idx] = (byte) (wave * env * 70);
					}
				}
				playBuffer(buf, sampleRate);
			} catch (Exception ignored) {}
		});
	}

	/** 提醒音效：两声短促的叮咚 */
	public static void playReminder() {
		if (!enabled) return;
		playAsync(() -> {
			try {
				int sampleRate = 22050;
				int beepLen = sampleRate / 8;
				int gapLen = sampleRate / 12;
				int total = beepLen * 2 + gapLen;
				byte[] buf = new byte[total];
				for (int b = 0; b < 2; b++) {
					int offset = b * (beepLen + gapLen);
					double freq = b == 0 ? 880.0 : 1108.73; // A5, C#6
					for (int i = 0; i < beepLen; i++) {
						double t = (double) i / sampleRate;
						double env = Math.min(1.0, (beepLen - i) / (double) (beepLen / 2));
						env = Math.min(env, i / (double) (sampleRate / 200));
						double wave = Math.sin(2 * Math.PI * freq * t);
						buf[offset + i] = (byte) (wave * env * 60);
					}
				}
				playBuffer(buf, sampleRate);
			} catch (Exception ignored) {}
		});
	}

	/** 开局音效：两个清亮的上行音，像棋子碰撞 */
	public static void playGameStart() {
		if (!enabled) return;
		playAsync(() -> {
			try {
				int sampleRate = 22050;
				int noteLen = sampleRate / 8;
				int gapLen = sampleRate / 20;
				int total = noteLen * 2 + gapLen;
				byte[] buf = new byte[total];
				double[] freqs = {660.0, 880.0}; // E5, A5
				for (int n = 0; n < 2; n++) {
					int offset = n * (noteLen + gapLen);
					for (int i = 0; i < noteLen; i++) {
						double t = (double) i / sampleRate;
						double env = Math.exp(-t * 20);
						env *= Math.min(1.0, i / (double) (sampleRate / 200));
						double wave = Math.sin(2 * Math.PI * freqs[n] * t) * 0.7
								+ Math.sin(2 * Math.PI * freqs[n] * 2 * t) * 0.3;
						buf[offset + i] = (byte) (wave * env * 80);
					}
				}
				playBuffer(buf, sampleRate);
			} catch (Exception ignored) {}
		});
	}

	/** 平局音效：平缓的单音渐弱 */
	public static void playDraw() {
		if (!enabled) return;
		playAsync(() -> {
			try {
				int sampleRate = 22050;
				int samples = sampleRate / 2;
				byte[] buf = new byte[samples];
				for (int i = 0; i < samples; i++) {
					double t = (double) i / sampleRate;
					double env = Math.exp(-t * 4);
					double wave = Math.sin(2 * Math.PI * 440 * t);
					buf[i] = (byte) (wave * env * 60);
				}
				playBuffer(buf, sampleRate);
			} catch (Exception ignored) {}
		});
	}

	private static void playBuffer(byte[] buf, int sampleRate) throws Exception {
		AudioFormat fmt = new AudioFormat(sampleRate, 8, 1, true, false);
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
		if (!AudioSystem.isLineSupported(info)) return;
		SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
		line.open(fmt);
		line.start();
		line.write(buf, 0, buf.length);
		line.drain();
		line.close();
	}

	private static void playAsync(Runnable task) {
		Thread t = new Thread(task);
		t.setDaemon(true);
		t.start();
	}
}
