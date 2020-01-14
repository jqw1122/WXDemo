package com.shijialiuxue;

public class NodeWord implements Comparable<NodeWord> {
    /**
     * 词
     */
    private String word;
    /**
     * 词 频率
     */
    private Integer frequency;

    public NodeWord(String word, Integer frequency) {
        this.word = word;
        this.frequency = frequency;
    }

    public String getWord() {
        return this.word;
    }

    public int getFrequency() {
        return this.frequency;
    }

    @Override
    public int compareTo(NodeWord node) {
        return this.frequency.compareTo(node.getFrequency());
    }

    /**
     * 词频加一
     */
    public void increment() {
        this.frequency += 1;
    }

    /**
     * 词频加n
     */
    public void increment(int num) {
        this.frequency += num;
    }
}
