package filtration.mutants;

import cn.edu.ustb.www.aviationconsignment.model.Passanger;
import logrecorder.RTLog;
import mutantSet.MutantSet;
import mutantSet.TestMethods;
import testcases.Bean;
import testcases.GenerateTestcases;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/***
 * this class is responsible to delete some mutants that are easy to be killed
 */
public class FiltrationMutants {
    /**the number of test suites*/
    private static final int SEEDS = 50 ;

    /**the total number of test cases needed to be generated*/
    private static final int NUMOFTESTCASES = 30000;

    /**the package name of source program*/
    private static final String ORIGINAL_PACKAGE = "cn.edu.ustb.www.aviationconsignment";

    /**
     * this method is responsible to execute all mutants and record the number of test cases needed to kill those
     * mutants
     */
    public void test(){
        // test cases generator
        GenerateTestcases generateTestcases = new GenerateTestcases();

        // the method names of SUT
        TestMethods testMethods = new TestMethods();
        List<String> methodsList = testMethods.getMethods();

        //
        RTLog rtLog = new RTLog("filtrationResult.txt");

        for (int i = 0; i < SEEDS; i++) {
            // generate test suite
            List<Bean> beans = new ArrayList<Bean>();
            beans.clear();
            beans = generateTestcases.generateTestcases(i,NUMOFTESTCASES);

            // mutants set
            MutantSet ms = new MutantSet();

            for (int j = 0; j < ms.size(); j++) {
                int counter = 0;

                for (int k = 0; k < beans.size(); k++) {
                    // get a test case
                    Bean bean = beans.get(k);
                    Passanger p1 = new Passanger();
                    p1.setAirlineType(Integer.parseInt(bean.getAirlineType()));
                    p1.setPassangerCategory(Integer.parseInt(bean.getPassangerCatagory()));
                    p1.setBaggageWeight(Double.parseDouble(bean.getBaggageWeight()));
                    p1.setCabinClass(Integer.parseInt(bean.getCabinClass()));
                    p1.setEconomyClassFare(Double.parseDouble(bean.getEconomyClassFare()));

                    try{
                        // get the instances for source and mutant, respectively
                        Class originalClazz = Class.forName(ORIGINAL_PACKAGE+".model."+"BaggageController");
                        Constructor constructor1 = originalClazz.getConstructor(Passanger.class);
                        Object originalInstance = constructor1.newInstance(p1);
                        Class mutantClazz = Class.forName(ms.getMutantName(j));
                        Constructor constructor2 = mutantClazz.getConstructor(Passanger.class);
                        Object mutantInstance = constructor2.newInstance(p1);

                        for (int l = 0; l < methodsList.size(); l++) {
                            Method originalMethod = originalClazz.getMethod(methodsList.get(l), null);
                            Object originalResult = originalMethod.invoke(originalInstance, null);
                            Method mutantMethod = mutantClazz.getMethod(methodsList.get(l), null);
                            Object mutantResult = mutantMethod.invoke(mutantInstance, null);

                            if (!originalResult.equals(mutantResult)) {
                                counter++;
                            }
                        }
                    }catch  (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
                rtLog.record(ms.getMutantName(j), counter);
            }
        }
    }

    public static void main(String[] args) {
        FiltrationMutants filtrationMutants = new FiltrationMutants();
        filtrationMutants.test();
    }


}
