package com.shijialiuxue;
/*

   Porter stemmer in Java. The original paper is in

       Porter, 1980, An algorithm for suffix stripping, Program, Vol. 14,
       no. 3, pp 130-137,

   See also http://www.tartarus.org/~martin/PorterStemmer

   History:

   Release 1

   Bug 1 (reported by Gonzalo Parra 16/10/99) fixed as marked below.
   The words 'aed', 'eed', 'oed' leave k at 'a' for step 3, and b[k-1]
   is then out outside the bounds of b.

   Release 2

   Similarly,

   Bug 2 (reported by Steve Dyrdahl 22/2/00) fixed as marked below.
   'ion' by itself leaves j = -1 in the test for 'ion' in step 5, and
   b[j] is then outside the bounds of b.

   Release 3

   Considerably revised 4/9/00 in the light of many helpful suggestions
   from Brian Goetz of Quiotix Corporation (brian@quiotix.com).

   Release 4

*/


import com.shijialiuxue.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Stemmer, implementing the Porter Stemming Algorithm
 * <p>
 * The Stemmer class transforms a word into its root form.  The input
 * word can be provided a character at time (by calling add()), or at once
 * by calling one of the various stem(something) methods.
 *
 * 一下所有方法都是算法部分，运行部分在main
 */
class PorterStemmer {
    private char[] b;
    private int i,     /* offset into b */
            i_end, /* offset to end of stemmed word */
            j, k;
    private static final int INC = 50;

    /* unit of size whereby b is increased */
    public PorterStemmer() {
        b = new char[INC];
        i = 0;
        i_end = 0;
    }

    /**
     * Add a character to the word being stemmed.  When you are finished
     * adding characters, you can call stem(void) to stem the word.
     */

    public void add(char ch) {
        if (i == b.length) {
            char[] new_b = new char[i + INC];
            for (int c = 0; c < i; c++) new_b[c] = b[c];
            b = new_b;
        }
        b[i++] = ch;
    }


    /** Adds wLen characters to the word being stemmed contained in a portion
     * of a char[] array. This is like repeated calls of add(char ch), but
     * faster.
     */

    private void add(char[] w, int wLen) {
        if (i + wLen >= b.length) {
            char[] new_b = new char[i + wLen + INC];
            for (int c = 0; c < i; c++) new_b[c] = b[c];
            b = new_b;
        }
        for (int c = 0; c < wLen; c++) b[i++] = w[c];
    }

    /**
     * After a word has been stemmed, it can be retrieved by toString(),
     * or a reference to the internal buffer can be retrieved by getResultBuffer
     * and getResultLength (which is generally more efficient.)
     */
    public String toString() {
        return new String(b, 0, i_end);
    }

    /**
     * Returns the length of the word resulting from the stemming process.
     */
    public int getResultLength() {
        return i_end;
    }

    /**
     * Returns a reference to a character buffer containing the results of
     * the stemming process.  You also need to consult getResultLength()
     * to determine the length of the result.
     */
    public char[] getResultBuffer() {
        return b;
    }

    /* cons(i) is true <=> b[i] is a consonant. */

    private boolean cons(int i) {
        switch (b[i]) {
            case 'a':
            case 'e':
            case 'i':
            case 'o':
            case 'u':
                return false;
            case 'y':
                return (i == 0) || !cons(i - 1);
            default:
                return true;
        }
    }

   /* m() measures the number of consonant sequences between 0 and j. if c is
      a consonant sequence and v a vowel sequence, and <..> indicates arbitrary
      presence,

         <c><v>       gives 0
         <c>vc<v>     gives 1
         <c>vcvc<v>   gives 2
         <c>vcvcvc<v> gives 3
         ....
   */

    private int m() {
        int n = 0;
        int i = 0;
        while (true) {
            if (i > j) return n;
            if (!cons(i)) break;
            i++;
        }
        i++;
        while (true) {
            while (true) {
                if (i > j) return n;
                if (cons(i)) break;
                i++;
            }
            i++;
            n++;
            while (true) {
                if (i > j) return n;
                if (!cons(i)) break;
                i++;
            }
            i++;
        }
    }

    /* vowelinstem() is true <=> 0,...j contains a vowel */

    private boolean vowelinstem() {
        int i;
        for (i = 0; i <= j; i++) if (!cons(i)) return true;
        return false;
    }

    /* doublec(j) is true <=> j,(j-1) contain a double consonant. */

    private boolean doublec(int j) {
        if (j < 1) return false;
        if (b[j] != b[j - 1]) return false;
        return cons(j);
    }

   /* cvc(i) is true <=> i-2,i-1,i has the form consonant - vowel - consonant
      and also if the second c is not w,x or y. this is used when trying to
      restore an e at the end of a short word. e.g.

         cav(e), lov(e), hop(e), crim(e), but
         snow, box, tray.

   */

    private boolean cvc(int i) {
        if (i < 2 || !cons(i) || cons(i - 1) || !cons(i - 2)) return false;
        {
            int ch = b[i];
            if (ch == 'w' || ch == 'x' || ch == 'y') return false;
        }
        return true;
    }

    private boolean ends(String s) {
        int l = s.length();
        int o = k - l + 1;
        if (o < 0) return false;
        for (int i = 0; i < l; i++) if (b[o + i] != s.charAt(i)) return false;
        j = k - l;
        return true;
    }

   /* setto(s) sets (j+1),...k to the characters in the string s, readjusting
      k. */

    private void setto(String s) {
        int l = s.length();
        int o = j + 1;
        for (int i = 0; i < l; i++) b[o + i] = s.charAt(i);
        k = j + l;
    }

    /* r(s) is used further down. */

    private void r(String s) {
        if (m() > 0) setto(s);
    }

   /* step1() gets rid of plurals and -ed or -ing. e.g.

          caresses  ->  caress
          ponies    ->  poni
          ties      ->  ti
          caress    ->  caress
          cats      ->  cat

          feed      ->  feed
          agreed    ->  agree
          disabled  ->  disable

          matting   ->  mat
          mating    ->  mate
          meeting   ->  meet
          milling   ->  mill
          messing   ->  mess

          meetings  ->  meet

   */

    private void step1() {
        if (b[k] == 's') {
            if (ends("sses")) k -= 2;
            else if (ends("ies")) setto("i");
            else if (b[k - 1] != 's') k--;
        }
        if (ends("eed")) {
            if (m() > 0) k--;
        } else if ((ends("ed") || ends("ing")) && vowelinstem()) {
            k = j;
            if (ends("at")) setto("ate");
            else if (ends("bl")) setto("ble");
            else if (ends("iz")) setto("ize");
            else if (doublec(k)) {
                k--;
                {
                    int ch = b[k];
                    if (ch == 'l' || ch == 's' || ch == 'z') k++;
                }
            } else if (m() == 1 && cvc(k)) setto("e");
        }
    }

    /* step2() turns terminal y to i when there is another vowel in the stem. */

    private void step2() {
        if (ends("y") && vowelinstem()) b[k] = 'i';
    }

   /* step3() maps double suffices to single ones. so -ization ( = -ize plus
      -ation) maps to -ize etc. note that the string before the suffix must give
      m() > 0. */

    private void step3() {
        if (k == 0) return; /* For Bug 1 */
        switch (b[k - 1]) {
            case 'a':
                if (ends("ational")) {
                    r("ate");
                    break;
                }
                if (ends("tional")) {
                    r("tion");
                    break;
                }
                break;
            case 'c':
                if (ends("enci")) {
                    r("ence");
                    break;
                }
                if (ends("anci")) {
                    r("ance");
                    break;
                }
                break;
            case 'e':
                if (ends("izer")) {
                    r("ize");
                    break;
                }
                break;
            case 'l':
                if (ends("bli")) {
                    r("ble");
                    break;
                }
                if (ends("alli")) {
                    r("al");
                    break;
                }
                if (ends("entli")) {
                    r("ent");
                    break;
                }
                if (ends("eli")) {
                    r("e");
                    break;
                }
                if (ends("ousli")) {
                    r("ous");
                    break;
                }
                break;
            case 'o':
                if (ends("ization")) {
                    r("ize");
                    break;
                }
                if (ends("ation")) {
                    r("ate");
                    break;
                }
                if (ends("ator")) {
                    r("ate");
                    break;
                }
                break;
            case 's':
                if (ends("alism")) {
                    r("al");
                    break;
                }
                if (ends("iveness")) {
                    r("ive");
                    break;
                }
                if (ends("fulness")) {
                    r("ful");
                    break;
                }
                if (ends("ousness")) {
                    r("ous");
                    break;
                }
                break;
            case 't':
                if (ends("aliti")) {
                    r("al");
                    break;
                }
                if (ends("iviti")) {
                    r("ive");
                    break;
                }
                if (ends("biliti")) {
                    r("ble");
                    break;
                }
                break;
            case 'g':
                if (ends("logi")) {
                    r("log");
                    break;
                }
        }
    }

    /* step4() deals with -ic-, -full, -ness etc. similar strategy to step3. */

    private void step4() {
        switch (b[k]) {
            case 'e':
                if (ends("icate")) {
                    r("ic");
                    break;
                }
                if (ends("ative")) {
                    r("");
                    break;
                }
                if (ends("alize")) {
                    r("al");
                    break;
                }
                break;
            case 'i':
                if (ends("iciti")) {
                    r("ic");
                    break;
                }
                break;
            case 'l':
                if (ends("ical")) {
                    r("ic");
                    break;
                }
                if (ends("ful")) {
                    r("");
                    break;
                }
                break;
            case 's':
                if (ends("ness")) {
                    r("");
                    break;
                }
                break;
        }
    }

    /* step5() takes off -ant, -ence etc., in context <c>vcvc<v>. */

    private void step5() {
        if (k == 0) return; /* for Bug 1 */
        switch (b[k - 1]) {
            case 'a':
                if (ends("al")) break;
                return;
            case 'c':
                if (ends("ance")) break;
                if (ends("ence")) break;
                return;
            case 'e':
                if (ends("er")) break;
                return;
            case 'i':
                if (ends("ic")) break;
                return;
            case 'l':
                if (ends("able")) break;
                if (ends("ible")) break;
                return;
            case 'n':
                if (ends("ant")) break;
                if (ends("ement")) break;
                if (ends("ment")) break;
                /* element etc. not stripped before the m */
                if (ends("ent")) break;
                return;
            case 'o':
                if (ends("ion") && j >= 0 && (b[j] == 's' || b[j] == 't')) break;
                /* j >= 0 fixes Bug 2 */
                if (ends("ou")) break;
                return;
            /* takes care of -ous */
            case 's':
                if (ends("ism")) break;
                return;
            case 't':
                if (ends("ate")) break;
                if (ends("iti")) break;
                return;
            case 'u':
                if (ends("ous")) break;
                return;
            case 'v':
                if (ends("ive")) break;
                return;
            case 'z':
                if (ends("ize")) break;
                return;
            default:
                return;
        }
        if (m() > 1) k = j;
    }

    /* step6() removes a final -e if m() > 1. */

    private void step6() {
        j = k;
        if (b[k] == 'e') {
            int a = m();
            if (a > 1 || a == 1 && !cvc(k - 1)) k--;
        }
        if (b[k] == 'l' && doublec(k) && m() > 1) k--;
    }

    /** Stem the word placed into the Stemmer buffer through calls to add().
     * Returns true if the stemming process resulted in a word different
     * from the input.  You can retrieve the result with
     * getResultLength()/getResultBuffer() or toString().
     */
    private void stem() {
        k = i - 1;
        if (k > 1) {
            step1();
            step2();
            step3();
            step4();
            step5();
            step6();
        }
        i_end = k + 1;
        i = 0;
    }

    //检查scan的单词是不是动词的特殊变换，对照特殊变幻的Excel表格
//    public static int findIrregular(List<List<String>> irregular, String word) {
//        for (List<String> words : irregular) {
//            if (words.contains(word)) {
//                return irregular.indexOf(words);
//            }
//        }
//        return -1;
//    }

    /**
     * 原:检查scan的单词是不是动词的特殊变换，对照特殊变幻的Excel表格
     *
     * 根据给的词根excel 判断是否已经给出当前单词, 若有则返回当前词 词根 否则返回null
     */
    private static String findIrregular(List<List<String>> irregular, String word) {

//        Optional<List<String>> first = irregular.parallelStream().filter(t -> t.contains(word)).findFirst();
//        return first.isEmpty() ? null : first.get().get(0);

        for (List<String> words : irregular) {
            if (words.contains(word)) {
                return words.get(0);
            }
        }
        return null;
    }

//    //检查单词是不是已经存在在result这个priority queue里面
//    private static boolean checkResult(ArrayList<Node> result, String word, String stem) {
//        //如果这个词已经存在在result里面，则频率加一
//        for (Node node : result) {
//            for (NodeWord words : node.getWordList()) {
//                if (words.getWord().equals(word)) {
//                    words.increment();
//                    node.increment();
////                    result.remove(node);
////                    result.add(node);
//                    return true;
//                }
//            }
//            //如果这个词本身不存在，但是他的词根存在，那么词根频率加一，并且把这个词存在词根里
//            if (node.getStem().equals(stem)) {
//                node.getWordList().add(new NodeWord(word, 1));
//                node.increment();
////                result.remove(node);
////                result.add(node);
//                return true;
//            }
//        }
//        return false;
//    }

    /**
     * 检查是否存在 在已经构建的list中
     * @param result list
     * @param word 单词
     * @param stem 单词词根
     * @param num 单词出现次数
     */
    private static boolean checkWordIsExistInArray(ArrayList<Node> result, String word, String stem, Integer num) {
        //如果这个词已经存在在result里面，则频率加一
        for (Node node : result) {
            for (NodeWord words : node.getWordList()) {
                if (words.getWord().equals(word)) {
                    words.increment(num);
                    node.increment(num);
                    return true;
                }
            }
            //如果这个词本身不存在，但是他的词根存在，那么词根频率加一，并且把这个词存在词根里
            if (node.getStem().equals(stem)) {
                node.getWordList().add(new NodeWord(word, num));
                node.increment(num);
                return true;
            }
        }
        return false;
    }

    /**
     * five1.xls  是测试文件
     * irregular1.xls  是测试文件
     *
     */
    private final static String irregularFilePath = "irregular.xls";
    private final static String wordsForExcel = "five.xls";
    private final static String wordsForTxt = "reading.txt";

    /**
     * 以下是运行部分，运用read中的方法和上面的算法
     */
    public static void main(String[] args) throws Exception {

        long startTime = System.currentTimeMillis();
        PorterStemmer s = new PorterStemmer();

        //读取存不规则单词的表格
        File file1 = new File(PorterStemmer.class.getClassLoader().getResource(irregularFilePath).getPath());
        List<List<String>> irregular = FileUtils.readIrregular(file1);

        //读取文档（TPO资料，Excel形式）
        File file2 = new File(PorterStemmer.class.getClassLoader().getResource(wordsForExcel).getPath());
        List<String> wordList = FileUtils.readExcel(file2);

        //读取文档（TPO资料，TXT格式）
        File file3 = new File(PorterStemmer.class.getClassLoader().getResource(wordsForTxt).getPath());
        wordList.addAll(FileUtils.readTxt(file3));



        long seconds = (startTime - System.currentTimeMillis()) / 1000;
        System.out.println("read file used seconds " + seconds);


        //分组 求和
        Map<String, Long> collect = wordList.parallelStream().collect(Collectors.groupingBy(String::toString, Collectors.counting()));


        //储存结果
        ArrayList<Node> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : collect.entrySet()) {
            String word = entry.getKey();
            int num = entry.getValue().intValue();
            System.out.println(entry.getKey());

            // 词根
            String temp = word;
//            if (!(findIrregular(irregular, word) == -1)) {
//                temp = irregular.get(findIrregular(irregular, temp)).get(0);
//            }
            String s1 = findIrregular(irregular, word);
            if (s1 != null) {
                temp = s1;
            }

            char[] wordsChar = temp.toCharArray();
//            int length = temp.length();
//            char[] wordsChar = new char[length];
//            //把单词改成char【】形式进行算法运算
//            for (int i = 0; i < length; ++i) {
//                wordsChar[i] = temp.charAt(i);
//            }
            s.add(wordsChar, wordsChar.length);
            s.stem();
            temp = s.toString();
            //如果这个词和他的词根在result里面从未存过，新建node存入
            if (!checkWordIsExistInArray(result, word, temp, num)) {
                // 词根-词根下的词-词根频率
                result.add(new Node(temp, new NodeWord(word, num), 1));
            }
        }

//        //过一遍所有的词，用算法把词根求出，并存入result
//        for (String word : wordList) {
//            System.out.println(word);
//            String temp = word;
////            if (!(findIrregular(irregular, word) == -1)) {
////                temp = irregular.get(findIrregular(irregular, temp)).get(0);
////            }
//            String s1 = findIrregular(irregular, word);
//            if (s1 != null) {
//                temp = s1;
//            }
//
//            char[] wordsChar = temp.toCharArray();
////            int length = temp.length();
////            char[] wordsChar = new char[length];
////            //把单词改成char【】形式进行算法运算
////            for (int i = 0; i < length; ++i) {
////                wordsChar[i] = temp.charAt(i);
////            }
//            s.add(wordsChar, wordsChar.length);
//            s.stem();
//            temp = s.toString();
//            //如果这个词和他的词根在result里面从未存过，新建node存入
//            if (!checkResult(result, word, temp)) {
//                result.add(new Node(temp, new Node_word(word, 1), 1));
//            }
//        }

        result.sort(Comparator.comparing(Node::getStem));
        //用write方法写入Excel
        FileUtils.makeExcel(result);

        System.out.println("used seconds " + (System.currentTimeMillis() - startTime) / 1000);
    }
}
