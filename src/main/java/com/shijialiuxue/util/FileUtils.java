package com.shijialiuxue.util;

import com.shijialiuxue.Node;
import com.shijialiuxue.NodeWord;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileUtils {

    /**
     * 读取excel中单词
     * 每一行为一组单词 eg.  abide和abode和abided 为一组单词
     * abide	abode	abided
     * alight	alighted	alit
     * arise	arose
     * @param file 文件
     * @return List<List<String>> list<string> 中是差不多相同的单词
     */
    public static List<List<String>> readIrregular(File file) throws Exception {
        //打开
        InputStream is = new FileInputStream(file);
        Workbook wb = Workbook.getWorkbook(is);
        Sheet sheet = wb.getSheet(0);
        int rows = sheet.getRows();
        //和前面的区别在于用list套list的储存形式
        //读取的irregular文档是已知文档，里面的内容就是每一行第一个是动词原形，
        //之后是动词的过去式过去分词等特殊变化形式
        //外面的大list存每一个不同单词，里面的小list存的是每一个单词的不同变形
        List<List<String>> list = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            Cell[] row = sheet.getRow(i);
            List<String> lList = new ArrayList<>();
            for (Cell cell : row) {
                String temp = cell.getContents().trim();
                if (isWord(temp)){
                    lList.add(toLowerCase(temp));
                }
            }
            list.add(lList);
        }

        return list;
    }


    /**
     * 读取Excel中的文字  转换成单词
     */
    public static List<String> readExcel(File file) throws Exception {
        InputStream is = new FileInputStream(file);
        Workbook wb = Workbook.getWorkbook(is);
        Sheet sheet = wb.getSheet(0);

        List<String> allData = new ArrayList<>();

        int rows = sheet.getRows();

        for (int i = 0; i < rows; i++) {
            Cell[] row = sheet.getRow(i);
            for (Cell cell : row) {
                sentence2words(allData, cell.getContents());
            }
        }

        return allData;
    }

    /**
     * 读取TXT中的单词
     */
    public static List<String> readTxt(File file) throws IOException {

        InputStreamReader read = null;
        BufferedReader bufferedReader = null;
        try {
            read = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);// 考虑到编码格式
            bufferedReader = new BufferedReader(read);

            List<String> allData = new ArrayList<>();
            String lineTxt ;
            while ((lineTxt = bufferedReader.readLine()) != null) {
                sentence2words(allData, lineTxt);
            }

            return allData;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (read != null) {
                try {
                    read.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    //用来把已经整理好的单词写到Excel当中去，放进来一个priority queue
    public static void makeExcel(ArrayList<Node> result) {
        //打开Excel
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("result");
        HSSFRow row = sheet.createRow(0);
        //表头
        HSSFCell cell = row.createCell(0);
        cell.setCellValue("stem");
        cell = row.createCell(1);
        cell.setCellValue("word");
        cell = row.createCell(2);
        cell.setCellValue("frequency");
        cell = row.createCell(3);
        cell.setCellValue("frequency in total");
        int i = 1; //记录行数
        HSSFRow row1; //记录新建的row
        //把priority queue里面的东西一个一个poll出来，写到Excel里
        for (Node node : result) {
            for (NodeWord word : node.getWordList()) {
                row1 = sheet.createRow(i++);
                row1.createCell(0).setCellValue(node.getStem());
                row1.createCell(1).setCellValue(word.getWord());
                row1.createCell(2).setCellValue(word.getFrequency());
                row1.createCell(3).setCellValue(node.getFrequency());
            }
            sheet.createRow(i++);
        }

        //写file固定程序
        try {
            File file = new File(FileUtils.class.getClassLoader().getResource("").getPath() + "\\result-all(copy)1.xls");
            if (file.exists()){
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            System.out.println("success");
            fos.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static boolean isWord(String str){
        return str.trim().matches("[a-zA-Z]+");
    }

    private static void sentence2words(List<String> allData, String str){
        if (str.length() == 0) {
            return;
        }
        str = toLowerCase(str);
        str = str.replace("'s", "");
        str = str.replaceAll("[^a-zA-Z]", " ");
        String[] wordList = str.trim().split("\\s+");

        allData.addAll(Arrays.asList(wordList));
    }


    private static String toLowerCase(String str){
        return str.toLowerCase();
    }
}
