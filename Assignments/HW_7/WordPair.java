import java.lang.Comparable;

public class WordPair implements Comparable<WordPair> {
    private double relativeFrequency;
    private String wordA;
    private String wordB;

    WordPair(double relativeFrequency, String wordA, String wordB) {
        this.relativeFrequency = relativeFrequency;
        this.wordA = wordA;
        this.wordB = wordB;
    }

    public int compareTo(WordPair other) {
        if (this.relativeFrequency < other.relativeFrequency) {
            return 1;
        } else if (this.relativeFrequency > other.relativeFrequency) {
            return -1;
        } else {
            return 0;
        }
    }

    public String toString() {
        return wordA + ", " + wordB + ", " + relativeFrequency;
    }
}



