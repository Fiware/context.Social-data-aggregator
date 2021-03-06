package com.tilab.ca.sda.gra_core.components;

import com.tilab.ca.sda.gra_core.GenderTypes;
import com.tilab.ca.sda.gra_core.utils.GraConstants;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import com.tilab.ca.sda.ctw.utils.Utils;

public class NamesGenderMapDefaultImpl implements NamesGenderMap{

    private Map<String,GenderTypes> namesGenderMap=null;
    private static final String DEFAULT_FILE_SEPARATOR=",";
    private static final String DEFAULT_FILE_NAME="names_gender.txt";
    
    public NamesGenderMapDefaultImpl(Properties props){
        String confPath=Utils.Env.getConfsPathFromEnv(GraConstants.SDA_CONF_SYSTEM_PROPERTY, GraConstants.GRA_SYSTEM_PROPERTY);
        namesGenderMap=loadGenderMapFromFile(confPath+File.separator+DEFAULT_FILE_NAME,
                DEFAULT_FILE_SEPARATOR);  
    }
    
    public NamesGenderMapDefaultImpl(Map<String,GenderTypes> namesGenderMap){
        this.namesGenderMap=namesGenderMap;
    }
    
    public NamesGenderMapDefaultImpl(String nameGenderFilePath,String fileSeparator){
        namesGenderMap=loadGenderMapFromFile(nameGenderFilePath,fileSeparator);
    }
    
    
    
    @Override
    public GenderTypes getGender(String name) {
        return namesGenderMap.getOrDefault(name, GenderTypes.UNKNOWN);
    }

    @Override
    public GenderTypes getGenderLongestPrefixName(String name) {
        
        Optional<String> optName=namesGenderMap.keySet().stream().filter((currName)->name.startsWith(currName) || name.endsWith(currName))
                                        .sorted((c1,c2) -> Integer.compare(c2.length(), c1.length()))
                                        .findFirst();
        return optName.isPresent()?namesGenderMap.get(optName.get()):GenderTypes.UNKNOWN;
    }
    
    private Map<String,GenderTypes> loadGenderMapFromFile(String filePath,String fileSeparator){      
        try {
            return Files.lines(Paths.get(filePath)).collect(Collectors.toMap((row)-> row.split(fileSeparator)[0], 
                                                                             (row)-> GenderTypes.fromChar(row.split(fileSeparator)[1].charAt(0))));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
  
}
