import javax.sound.sampled.AudioFormat;
import java.io.Serializable;

public class RecordingFormat implements Serializable {
    private float sampleRate;
    private int sampleSizeInBits;
    private int channels;
    private boolean signed;
    private boolean bigEndian;

    public RecordingFormat(float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian) {
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channels = channels;
        this.signed = signed;
        this.bigEndian = bigEndian;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public int getSampleSizeInBits() {
        return sampleSizeInBits;
    }

    public int getChannels() {
        return channels;
    }

    public boolean isSigned() {
        return signed;
    }

    public boolean isBigEndian() {
        return bigEndian;
    }

    public AudioFormat getAudioFormat() {
        return new AudioFormat(this.getSampleRate(), this.getSampleSizeInBits(), this.getChannels(), this.isSigned(), this.isBigEndian());
    }
}
