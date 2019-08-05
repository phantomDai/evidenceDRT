package failureRate.partition;

import cn.edu.ustb.www.aviationconsignment.model.Passanger;
import mutantSet.MutantSet;
import mutantSet.TestMethods;
import partition.RPTPartition;
import testcases.Bean;
import testcases.GenerateTestcases;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static java.io.File.separator;

public class FailureRateOfPartition {
    //分区模式一下的分区个数
    private static final int PARTITIONSCHEMAONE = 24 ;

    //分区模式二下的分区个数
    private static final int PARTITIONSCHEMATWO = 7 ;

    //实验重复的次数
    private static final int TESTTIMES = 30 ;

    //随机数的种子数
    private static final int SEEDS = 30 ;

    //产生的测试用例数目
    private static final int NUMBEROFTESTCASES = 30000 ;

    //变异体所在的位置
    private static final String ORIGINAL_PACKAGE = "cn.edu.ustb.www.aviationconsignment" ;

    //变异体的数目
    private static final int NUMBEROFMUTANTS = 3 ;

    //记录分区模式1中每一个分区平均需要的测试用例数
    private double[] resultPartitionSchema1 = new double[PARTITIONSCHEMAONE] ;

    //记录分区模式1中每一个分区平均需要的测试用例数
    private double[] resultPartitionSchema2 = new double[PARTITIONSCHEMATWO] ;




    public void calculateFailureRate(){
        GenerateTestcases generateTestcases = new GenerateTestcases();//产生测试用例的对象

        RPTPartition rptPartition = new RPTPartition();//产生分区的对象

        int[] numOfpartition = {PARTITIONSCHEMAONE, PARTITIONSCHEMATWO};//记录分区的方式

        //创建一个记录测试对象方法的对象
        TestMethods testMethods = new TestMethods();
        List<String> methodsList = testMethods.getMethods();

        //变异体集合
        MutantSet ms = new MutantSet();//测试用例集对象

        for (int i = 0; i < numOfpartition.length; i++){ //两种分区方式
            //针对每一种分区方式进行循环
            for (int j = 0; j < numOfpartition[i]; j++) { //某一种分区方式
                System.out.println("分区模式" + String.valueOf(i+1) + "第" + String.valueOf(j) + "分区");
                RecordCount recordCount = new RecordCount();//记录该分区下在所有的粽子以及重复次数下的实验结果
                recordCount.removeAll();//每次测试之前清空列表中的所有元素
                for (int k = 0; k < SEEDS; k++) { //随机数的种子
                    int count = 0 ; //记录在一个分区中执行了多少的测试用例
                    for (int y = 0; y < TESTTIMES; y++) {//循环次数
                        boolean flag = false; //标志位该分区杀死了变异体或者到达了执行上限

                        List<Bean> beans = new ArrayList<Bean>();
                        beans.clear(); //每一次循环都要清空列表
                        beans = generateTestcases.generateTestcases(k,NUMBEROFTESTCASES);//生成测试用例
                        for (int w = 0; w < beans.size() && !flag; w++) { //逐个循环测试用例
                           //判断当前测试用例是否在指定的分区之中
                            Bean bean;
                            do {
                                bean = beans.get(w++);
                            }while(!rptPartition.isBelongToOneOfPartition(bean,numOfpartition[i],j));
                            count += 1; //在该分区的测试数目自增

                            //初始化实际的测试用例
                            Passanger p1 = new Passanger();
                            p1.setAirlineType(Integer.parseInt(bean.getAirlineType()));
                            p1.setPassangerCategory(Integer.parseInt(bean.getPassangerCatagory()));
                            p1.setBaggageWeight(Double.parseDouble(bean.getBaggageWeight()));
                            p1.setCabinClass(Integer.parseInt(bean.getCabinClass()));
                            p1.setEconomyClassFare(Double.parseDouble(bean.getEconomyClassFare()));


                                //逐个遍历变异体进行测试
                                try{
                                    for (int z = 0; z < ms.size() && !flag; z++){ //逐个测试变异体集中的每一个变异体
                                        //获取原始程序的实例
                                        Class originalClazz = Class.forName(ORIGINAL_PACKAGE+".model."+"BaggageController");
                                        Constructor constructor1 = originalClazz.getConstructor(Passanger.class);
                                        Object originalInstance = constructor1.newInstance(p1);
                                        //获取变异体程序的实例
                                        Class mutantClazz = Class.forName(ms.getMutantName(z));
                                        Constructor constructor2 = mutantClazz.getConstructor(Passanger.class);
                                        Object mutantInstance = constructor2.newInstance(p1);
                                        //对一个变异体的所有方法进行遍历
                                        for (int m = 0; m < methodsList.size() && !flag; m++) {
                                            //获取源程序的方法
                                            Method originalMethod = originalClazz.getMethod(methodsList.get(m), null);

                                            //获取源程序的测试结果
                                            Object originalResult = originalMethod.invoke(originalInstance, null);
                                            Method mutantMethod = mutantClazz.getMethod(methodsList.get(m), null);
                                            //获取目标程序的测试结果
                                            Object mutantResult = mutantMethod.invoke(mutantInstance, null);

                                            if(!originalResult.equals(mutantResult) || count >= 50){ //揭示了软件中的故障
                                                //记录此时该分区已经使用的测试用例数目
                                                if (count < 50) {
                                                    recordCount.add(count);
                                                    flag = true;//找到了故障，进行下一次重复试验
                                                }else {
                                                    recordCount.add(1000);
                                                    flag = true;//到达该分区的执行上限，认为该分区几乎没有揭示故障的能力
                                                }

                                            }

                                        }
                                    }
                                } catch (InstantiationException e) {
                                    e.printStackTrace();
                                } catch (InvocationTargetException e) {
                                    e.printStackTrace();
                                } catch (NoSuchMethodException e) {
                                    e.printStackTrace();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                } catch (ClassNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }


                    }//循环次数
                }//随机数的种子
                //将该分区的失效率记录
                if (i == 0){
                    resultPartitionSchema1[j] = 1 / recordCount.getMean();
                }else
                    resultPartitionSchema2[j] = 1 / recordCount.getMean();
            }//某一种分区方式
        }//两种分区方式

        //测试完记录数据
        writeResultTpTxt();
    }

    //将每一个分区的失效率写入到文件中
    private void writeResultTpTxt(){
        String path = System.getProperty("user.dir") + separator + "failureRate" + separator + "failureRate.txt" ;
        File file = new File(path);
        try{
            if (file.exists())
                file.delete();
            else
                file.createNewFile();

            //首先记录分区模式1中的失效率
            StringBuffer stringBuffer1 = new StringBuffer();
            stringBuffer1.append("分区模式1：" + "\r");
            for (int i = 0; i < resultPartitionSchema1.length; i++) {
                stringBuffer1.append(String.valueOf(i + 1) + ":" + "" + String.valueOf(resultPartitionSchema1[i]) + "\r");
            }

            //记录分区模式2中的结果
            StringBuffer stringBuffer2 = new StringBuffer();
            stringBuffer2.append("分区模式2：" + "\r");
            for (int i = 0; i < resultPartitionSchema2.length; i++) {
                stringBuffer2.append(String.valueOf(i + 1) + ":" + "" + String.valueOf(resultPartitionSchema2[i]) + "\r");
            }

            //向文件中写入内容
            FileWriter fileWriter = new FileWriter(path,true);
            fileWriter.write(stringBuffer1.toString() + stringBuffer2.toString());
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void test(){
        resultPartitionSchema1[0] = 0.01;
        resultPartitionSchema1[1] = 0.02;
        resultPartitionSchema2[0] = 0.01;
        resultPartitionSchema2[1] = 0.02;
        writeResultTpTxt();
    }


    public static void main(String[] args) {
        FailureRateOfPartition fr = new FailureRateOfPartition();
        fr.calculateFailureRate();
//        fr.test();
    }

}
