package com.payegis.tools.file;

import org.apache.log4j.Logger;

import java.util.*;

/**
 * company: 北京通付盾数据科技有限公司
 * user: chenzuoli
 * date: 2018/6/14
 * time: 14:36
 * description: 文本文件分隔符识别器
 * 1.首先自定义一个分隔符库，比如默认分隔符为,; \t，首先根据这几个分隔符去一一对文件的每一行进行切分，得到每行的列数，可以得出这个文件的列数离散图，
 * 列数越集中，即标准差越小，并且分出的列数最多，则该分隔符极有可能是该文件的分隔符。
 * 2.如果分隔符库中对文件分隔后的情况都不太好，那么可以手动指定分隔符，并添加到分隔符库中。
 */
public class FileDelimiterRecognizer {
    private static Logger logger = Logger.getLogger(FileDelimiterRecognizer.class);

    /**
     * description: 从数据文件中自动判断分隔符，丰富文件行分隔符库，然后返回最优分隔符
     * param: [delimiterDbPath, dataFilePath]
     * return: java.lang.String
     * time: 2018/6/19 10:53
     */
    public static String bestDelimiter(String delimiterDbPath, String dataFilePath) {
        Map<String, String> delimiter_SD_AvgC = new HashMap<>(); // 分隔符,标准差_字段均数
        List<String> delimiterList = FileUtils.readFile(delimiterDbPath);
        delimiterList.forEach(delimiter -> {
            String SD_Avg = fileStandardDeviation(dataFilePath, delimiter);
            delimiter_SD_AvgC.put(delimiter, SD_Avg);
            System.out.println("delimiter: " + delimiter + ", standard deviation and average column number: " + SD_Avg);
        });
        String bestDelimiter = bestDelimiter(delimiter_SD_AvgC);
        System.out.println("best delimiter is " + bestDelimiter);
        FileDivisionETL.saveDelimiter(delimiterDbPath, bestDelimiter); // 保存分隔符到分隔符库
        return bestDelimiter;
    }

    /**
     * description: 从计算的每个分隔符对该文件的标准差和平均字段数中获取标准差最小，平均字段数最大的分隔符
     * param: [delimiter_SD_AvgC]
     * return: java.lang.String
     * date: 2018/6/14
     * time: 17:08
     */
    private static String bestDelimiter(Map<String, String> delimiter_SD_AvgC) {
        String bestDelimiter = "";
        try {
            double maxSD = 0;
            double maxAvgC = 0;
            Set<String> delimiters = delimiter_SD_AvgC.keySet();
            for (String delimiter : delimiters) {
                String sd_avgc = delimiter_SD_AvgC.getOrDefault(delimiter, "");
                if (sd_avgc == null) continue;
                String[] split = sd_avgc.split("_");
                if (split.length == 2) {
                    double avgColumnNum = Double.parseDouble(split[1]);
                    double standardDeviation = Double.parseDouble(split[0]);
                    if (maxSD < standardDeviation) {
                        maxSD = standardDeviation;
                        bestDelimiter = delimiter;
                    } else if (maxSD == standardDeviation && maxAvgC < avgColumnNum) {
                        bestDelimiter = delimiter;
                    } else if (maxSD == standardDeviation && maxAvgC == avgColumnNum) {
                        bestDelimiter = delimiter;
                    }
                }
            }
        } catch (Exception e){
            logger.error("get best delimiter from " + delimiter_SD_AvgC + " exception!", e);
        }
        return bestDelimiter;
    }

    /**
     * description: 根据指定的分隔符对文件的每行进行切分，得到的列数求标准差、列数均值
     * param: [filePath, delimiter]
     * return: double
     * date: 2018/6/14
     * time: 15:25
     */
    private static String fileStandardDeviation(String filePath, String delimiter) {
        double sd = 0.0;
        double avg = 0.0;
        try {
            Set<String> lines = FileUtils.readFileDeDuplByTika(filePath); // tika读取文件，并进行去重
            List<Double> columnNum = new ArrayList<>();
            double sum = 0;
            for (String line : lines) {
                String[] splits = line.split(delimiter);
                sum += splits.length;
                columnNum.add((double) splits.length);
            }
            sd = standardDeviation(columnNum);
            if (lines.size() != 0) {
                avg = sum / lines.size();
            } else {
                avg = 0;
            }
        } catch (Exception e){
            logger.error("get file " + filePath + " standard deviation by delimiter " + delimiter + " exception!", e);
        }
        return sd + "_" + avg;
    }

    /**
     * description: 计算被指定分隔符分隔后的行的列数平均值
     * param: [filePath, delimiter]
     * return: double
     * date: 2018/6/14
     * time: 15:59
     */
    private static double avgColumnNum(String filePath, String delimiter) {
        double sum = 0;
        List<String> lines = new ArrayList<>();
        try {
            lines = FileUtils.readFile(filePath);
            for (String line : lines) {
                String[] splits = line.split(delimiter);
                sum += splits.length;
            }
        } catch (Exception e){
            logger.error("get file " + filePath + " average column num by delimiter " + delimiter + " exception!", e);
        }
        if (lines.size() != 0) {
            return sum / lines.size(); // 求平均值
        } else return 0;
    }

    /**
     * description: 方差s^2=[(x1-x)^2 +...(xn-x)^2]/n
     * param: [x]
     * return: double
     * date: 2018/6/14
     * time: 15:12
     */
    private static double variance(List<Double> x) {
        int m = x.size();
        double sum = 0;
        for (double d : x) { // 求和
            sum += d;
        }
        double dAve = sum / m; // 求平均值
        double dVar = 0;
        for (double d : x) { // 求方差
            dVar += (d - dAve) * (d - dAve);
        }
        return dVar / m;
    }

    /**
     * description: 标准差σ=sqrt(s^2)
     * param: [x]
     * return: double
     * date: 2018/6/14
     * time: 15:12
     */
    private static double standardDeviation(List<Double> x) {
        int m = x.size();
        double sum = 0;
        for (double d : x) { // 求和
            sum += d;
        }
        double dAve = sum / m; // 求平均值
        double dVar = 0;
        for (double d : x) { // 求方差
            dVar += (d - dAve) * (d - dAve);
        }
        return Math.sqrt(dVar / m);
    }

}
