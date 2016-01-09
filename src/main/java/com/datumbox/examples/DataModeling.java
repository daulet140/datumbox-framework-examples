/**
 * Copyright (C) 2013-2016 Vasilis Vryniotis <bbriniotis@datumbox.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datumbox.examples;

import com.datumbox.applications.datamodeling.Modeler;
import com.datumbox.common.dataobjects.Dataframe;
import com.datumbox.common.dataobjects.Record;
import com.datumbox.common.dataobjects.TypeInference;
import com.datumbox.common.persistentstorage.ConfigurationFactory;
import com.datumbox.common.persistentstorage.interfaces.DatabaseConfiguration;
import com.datumbox.common.utilities.PHPMethods;
import com.datumbox.common.utilities.RandomGenerator;
import com.datumbox.framework.machinelearning.common.interfaces.ValidationMetrics;
import com.datumbox.framework.machinelearning.datatransformation.DummyXYMinMaxNormalizer;
import com.datumbox.framework.machinelearning.regression.NLMS;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * DataModeling example.
 * 
 * @author Vasilis Vryniotis <bbriniotis@datumbox.com>
 */
public class DataModeling {
    
    /**
     * Example of how to use the Modeler class.
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args) {      
        /**
         * There are two configuration files in the resources folder:
         * 
         * - datumbox.config.properties: It contains the configuration for the storage engines (required)
         * - logback.xml: It contains the configuration file for the logger (optional)
         */
        
        //Initialization
        //--------------
        RandomGenerator.setGlobalSeed(42L); //optionally set a specific seed for all Random objects
        DatabaseConfiguration dbConf = ConfigurationFactory.INMEMORY.getConfiguration(); //in-memory maps
        //DatabaseConfiguration dbConf = ConfigurationFactory.MAPDB.getConfiguration(); //mapdb maps
        
        
        
        //Reading Data
        //------------
        Dataframe trainingDataframe;
        try (Reader fileReader = new InputStreamReader(new FileInputStream(Paths.get(Clustering.class.getClassLoader().getResource("datasets/labor-statistics/longley.csv").toURI()).toFile()), "UTF-8")) {
            Map<String, TypeInference.DataType> headerDataTypes = new HashMap<>();
            headerDataTypes.put("Employed", TypeInference.DataType.NUMERICAL);
            headerDataTypes.put("GNP.deflator", TypeInference.DataType.NUMERICAL);
            headerDataTypes.put("GNP", TypeInference.DataType.NUMERICAL);
            headerDataTypes.put("Unemployed", TypeInference.DataType.NUMERICAL);
            headerDataTypes.put("Armed.Forces", TypeInference.DataType.NUMERICAL);  
            headerDataTypes.put("Population", TypeInference.DataType.NUMERICAL);
            headerDataTypes.put("Year", TypeInference.DataType.NUMERICAL); 
            
            trainingDataframe = Dataframe.Builder.parseCSVFile(fileReader, "Employed", headerDataTypes, ',', '"', "\r\n", dbConf);
        }
        catch(UncheckedIOException | IOException | URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
        Dataframe testingDataframe = trainingDataframe.copy();
        
        
        
        //Setup Training Parameters
        //-------------------------
        Modeler.TrainingParameters trainingParameters = new Modeler.TrainingParameters();
        
        //Model Configuration
        trainingParameters.setMLmodelClass(NLMS.class);
        trainingParameters.setMLmodelTrainingParameters(new NLMS.TrainingParameters());

        //Set data transfomation configuration
        trainingParameters.setDataTransformerClass(DummyXYMinMaxNormalizer.class);
        trainingParameters.setDataTransformerTrainingParameters(new DummyXYMinMaxNormalizer.TrainingParameters());
        
        //Set feature selection configuration
        trainingParameters.setFeatureSelectionClass(null);
        trainingParameters.setFeatureSelectionTrainingParameters(null);
        
        
        
        //Fit the modeler
        //---------------
        Modeler modeler = new Modeler("LaborStatistics", dbConf);
        modeler.fit(trainingDataframe, trainingParameters);
        
        
        
        //Use the modeler
        //---------------
        
        //Get validation metrics on the training set
        ValidationMetrics vm = modeler.validate(trainingDataframe);
        modeler.setValidationMetrics(vm); //store them in the model for future reference
        
        //Predict a new Dataframe
        modeler.predict(testingDataframe);
        
        System.out.println("Test Results:");
        for(Map.Entry<Integer, Record> entry: testingDataframe.entries()) {
            Integer rId = entry.getKey();
            Record r = entry.getValue();
            System.out.println("Record "+rId+" - Real Y: "+r.getY()+", Predicted Y: "+r.getYPredicted());
        }
        
        System.out.println("Modeler Statistics: "+PHPMethods.var_export(vm));
        
        
        
        //Clean up
        //--------
        
        //Delete the modeler. This removes all files.
        modeler.delete();
        
        //Delete Dataframes.
        trainingDataframe.delete();
        testingDataframe.delete();
    }
    
}
