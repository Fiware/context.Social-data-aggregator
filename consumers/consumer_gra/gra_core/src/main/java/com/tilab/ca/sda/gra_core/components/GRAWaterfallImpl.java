package com.tilab.ca.sda.gra_core.components;

import com.tilab.ca.sda.gra_core.DescrResults;
import com.tilab.ca.sda.gra_core.GenderTypes;
import com.tilab.ca.sda.gra_core.ProfileGender;
import com.tilab.ca.sda.sda.model.TwUserProfile;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;


public class GRAWaterfallImpl implements GRA{
   
    private static final Logger log=Logger.getLogger(GRAWaterfallImpl.class);
    
    private GenderUserDescr genderUserDescr;
    private GenderUserColors genderUserColor;
    private GenderNameSN genderName;
    
    private boolean initialized=false;
    
    
    @Override
    public void init(GRAConfig conf, JavaSparkContext jsc) {
        if(!conf.areMandatoryFieldsFilled())
            throw new IllegalStateException("Missing required data in GraConfig");
        //init all the sub algorithms 
        log.info("create gra color recognizer..");
        genderUserColor=new GenderUserColors(conf.getNumBits(), conf.getNumColors(), conf.getColoursModel(), jsc, conf.getTrainingPathStr());
        
        log.info("create gra description recognizer..");
        genderUserDescr=new GenderUserDescr(conf.getDescrModel(), conf.getFe(), jsc, conf.getTrainingPathStr());
        
        log.info("create gra name/screenName recognizer..");
        genderName=new GenderNameSN(conf.getNamesGenderMap());
       
        initialized=true;
        log.info("GraWaterfall initialized successfully!");
    }
    
    @Override
    public JavaRDD<ProfileGender> evaluateProfiles(JavaRDD<TwUserProfile> twProfilesRdd){ //JavaSparkContext jsc
        log.info("getting gender from name and screenName..");
        
        JavaSparkContext jsc = JavaSparkContext.fromSparkContext(twProfilesRdd.context());
        
        
        if(!initialized)
            throw new IllegalStateException("GRA implementation not initialized. Please call init() method before evaluation.");
        
        JavaRDD<ProfileGender> namesGenderRDD=genderName.getNamesGenderRDD(twProfilesRdd);
        
        //filter profiles that are not recognized from the first algorithm
        JavaRDD<ProfileGender> notReconFromName=namesGenderRDD.filter(profileGender -> profileGender.getGender()==GenderTypes.UNKNOWN ||
                                                                                         profileGender.getGender()==GenderTypes.AMBIGUOUS);
        
        log.info("getting gender from description..");
        DescrResults descrResults=genderUserDescr.getGendersFromTwProfiles(notReconFromName);
        JavaRDD<ProfileGender> descrGenderRdd=descrResults.getProfilesRecognized();
               
        JavaRDD<ProfileGender> notReconFromDescr=descrResults.getProfilesUnrecognized().filter(profileGender -> profileGender.getGender()==GenderTypes.UNKNOWN);
        
        log.info("getting gender from colors..");
        JavaRDD<ProfileGender> colorGenderRdd=genderUserColor.getGendersFromTwProfiles(notReconFromDescr);
        
        namesGenderRDD=namesGenderRDD.filter(profileGender -> profileGender.getGender()!=GenderTypes.UNKNOWN &&
                                                               profileGender.getGender()!=GenderTypes.AMBIGUOUS);
        
        
        return jsc.union(namesGenderRDD,descrGenderRdd,colorGenderRdd);
    }

}
