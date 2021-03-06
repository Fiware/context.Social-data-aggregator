package com.tilab.ca.sda.consumer.tw.tot.batch;

import com.tilab.ca.sda.consumer.tw.tot.batch.utils.Arguments;
import com.tilab.ca.sda.consumer.utils.BatchUtils;
import com.tilab.ca.sda.consumer.tw.tot.batch.utils.CommandLineArgs;
import com.tilab.ca.sda.consumer.tw.tot.batch.utils.TwTotConsumerProperties;
import com.tilab.ca.sda.consumer.tw.tot.core.TotTwConstants;
import com.tilab.ca.sda.consumer.tw.tot.core.TwCounter;
import com.tilab.ca.sda.sda.model.keys.DateHtKey;
import com.tilab.ca.sda.sda.model.keys.GeoLocTruncKey;
import com.tilab.ca.sda.sda.model.keys.GeoLocTruncTimeKey;
import com.tilab.ca.sda.consumer.tw.tot.core.data.StatsCounter;
import com.tilab.ca.sda.consumer.tw.tot.dao.ConsumerTwTotDao;
import com.tilab.ca.sda.ctw.utils.RoundManager;
import com.tilab.ca.sda.ctw.utils.Utils;
import com.tilab.ca.sda.sda.model.GeoStatus;
import com.tilab.ca.sda.sda.model.HtsStatus;
import java.io.File;
import java.util.Properties;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;



public class TwTotConsumerBatchMain {
    
    private static final Logger log=Logger.getLogger(TwTotConsumerBatchMain.class);
    
    private static final String APP_NAME="totTwConsumerBatch";
    private static final String DAO_IMPL_CONF_FILE_NAME="dao_impl.conf";
    
    
    public static void main(String[] args){
      
        try {
            String confsPath=Utils.Env.getConfsPathFromEnv(TotTwConstants.SDA_CONF_SYSTEM_PROPERTY, TotTwConstants.TOT_TW_SYSTEM_PROPERTY);
            String log4jPropsFilePath=confsPath+File.separator+TotTwConstants.LOG4jPROPS_FILE_NAME;
            PropertyConfigurator.configure(log4jPropsFilePath);
            
            log.info("Start Tw tot Consumer Batch");
            log.info("Parsing commandline arguments...");
            
            Arguments arguments=CommandLineArgs.parseCommandLineArgs(args);
            TwTotConsumerProperties twProps=loadProps(confsPath);
            
            log.info("Loading tw tot DAO..");
            ConsumerTwTotDao twDao=loadConsumerTwTotDao(confsPath, twProps.daoImplClass());
         
            SparkConf conf=new SparkConf().setAppName(APP_NAME);
	    JavaSparkContext sc=new JavaSparkContext(conf);
            String inputDataPath= StringUtils.isBlank(arguments.getInputDataPath())?twProps.defaultInputDataPath():arguments.getInputDataPath();
            if(StringUtils.isBlank(inputDataPath))
                throw new IllegalArgumentException("Input file cannot be blank. Please provide it on the properties file or by commandline argument");
            
            log.debug("Input data path is "+inputDataPath);
            executeTotTwAnalysis(sc, inputDataPath, twProps, arguments, twDao);
            sc.stop();
            log.info("Tw tot Consumer Batch END");
        } catch (Exception ex) {
            log.error(ex);
        }
    }

    private static void executeTotTwAnalysis(JavaSparkContext sc, String inputDataPath, TwTotConsumerProperties twProps, Arguments arguments, ConsumerTwTotDao twDao) {
        JavaRDD<String> tweetsRdd=sc.textFile(inputDataPath, twProps.minPartitions());
        
        final int roundPos=twProps.roundPos();
        JavaRDD<GeoStatus> geoStatus=tweetsRdd
                .filter(BatchUtils::isGeoLocStatus)
                .map((tweetStr) -> BatchUtils.fromJstring2GeoStatus(tweetStr, roundPos))
                .filter((geo) -> geo.getPostId()>0); //filter void statuses
                
        if(arguments.getRoundMode()!=null){
            JavaPairRDD<GeoLocTruncTimeKey, StatsCounter> pairTotRDDGeoRound=TwCounter.countGeoStatuses(geoStatus, arguments.getRoundMode(), arguments.getGranMin(), arguments.getFrom(), arguments.getTo());
            twDao.saveGeoByTimeGran(pairTotRDDGeoRound,RoundManager.getGranMinFromRoundType(arguments.getRoundMode(), arguments.getGranMin()));
        }else{
            JavaPairRDD<GeoLocTruncKey, StatsCounter>  pairTotGeoRDD=TwCounter.countGeoStatusesFromTimeBounds(geoStatus, arguments.getFrom(), arguments.getTo());
            twDao.saveGeoByTimeInterval(Utils.Time.zonedDateTime2Date(arguments.getFrom()), Utils.Time.zonedDateTime2Date(arguments.getTo()), pairTotGeoRDD);
        }
        
        JavaRDD<HtsStatus> htsStatus=tweetsRdd
                .filter(BatchUtils::isHtsStatus)
                .flatMap(BatchUtils::fromJstring2HtsStatus);
                    
        if(arguments.getRoundMode()!=null){
            JavaPairRDD<DateHtKey, StatsCounter> pairTotRDDHtsRound=TwCounter.countHtsStatuses(htsStatus, arguments.getRoundMode(), arguments.getGranMin(), arguments.getFrom(), arguments.getTo());
            twDao.saveHtsByTimeGran(pairTotRDDHtsRound,RoundManager.getGranMinFromRoundType(arguments.getRoundMode(), arguments.getGranMin()));
        }else{
            JavaPairRDD<String, StatsCounter> pairTotHtsRDD=TwCounter.countHtsStatusesFromTimeBounds(htsStatus, arguments.getFrom(), arguments.getTo());
            twDao.saveHtsByTimeInterval(Utils.Time.zonedDateTime2Date(arguments.getFrom()), Utils.Time.zonedDateTime2Date(arguments.getTo()), pairTotHtsRDD);
        }
    }
    
    private static ConsumerTwTotDao loadConsumerTwTotDao(String confsPath,String implClassStr) throws Exception {
        Properties props=Utils.Load.loadPropertiesFromPath(confsPath+File.separator+DAO_IMPL_CONF_FILE_NAME);
        props.put(ConsumerTwTotDao.CONF_PATH_PROPS_KEY, confsPath);
        return Utils.Load.getClassInstFromInterface(ConsumerTwTotDao.class, implClassStr, props);
    }
    
    private static TwTotConsumerProperties loadProps(String confsPath) throws Exception{
        TwTotConsumerProperties twProps=ConfigFactory.create(TwTotConsumerProperties.class);
        if(twProps==null){
            Properties props = Utils.Load.loadPropertiesFromPath(confsPath);
            twProps = ConfigFactory.create(TwTotConsumerProperties.class, props);
        }
        return twProps;
    }
}
