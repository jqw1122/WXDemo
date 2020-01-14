package com.shijialiuxue;

import java.util.ArrayList;
import java.util.List;

public class Node implements Comparable<Node> {
    /**
     * 词根
     */
    private String stem;

    /**
     * 词根 频率
     */
    private Integer frequency;

    /**
     * 词根对应词
     */
    private List<NodeWord> wordList;

    public Node(String stem, NodeWord wordList, Integer frequency) {
        this.stem = stem;
        this.wordList = new ArrayList<>();
        this.wordList.add(wordList);
        this.frequency = frequency;
    }

    public String getStem() {
        return stem;
    }

    public List<NodeWord> getWordList() {
        return wordList;
    }

    public int getFrequency() {
        return frequency;
    }

    @Override
    public int compareTo(Node node) {
        return this.frequency.compareTo(node.getFrequency());
    }

    /**
     * 词根 频率加一
     */
    public void increment() {
        frequency++;
    }

    /**
     * 词频加n
     */
    public void increment(int num) {
        this.frequency += num;
    }
}
