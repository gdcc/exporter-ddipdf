# exporter-ddipdf
Provides a PDF format for the DDI metadata
# Installation
Create jvm-options in dataverse for exporters, where /var/lib/dataverse/exporters is an example of a directory for exporters, you can choose any directory you want to keep external exporters in:
```  
  ./asadmin create-jvm-options '-Ddataverse.spi.exporters.directory=/var/lib/dataverse/exporters'
```
Create a jar file by compiling the exporter-ddipdf
```  
  mvn assembly:assembly -DdescriptorId=jar-with-dependencies
```
Copy the created jar file in target directory to the external exporters' directory that you defined in jvm-options.
