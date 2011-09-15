---
**Ocean Observatories Initiative Cyberinfrastructure**  
**Integrated Observatory Network (ION)**  
**THREDDS**  

---

# Description

#Source

Obtain the THREDDS project by running:  

    git clone -b develop git@github.com:ooici-eoi/THREDDS.git
    cd THREDDS
    git submodule update --init


###Pre-compilation
Only need to do once or after modifications to base NetCDF component libraries

From THREDDS directory:  
>1. mvn install  

>1. IF you make changes to any of the core NetCDF libraries (in the 'cdm', 'opendap', 'bufr', 'common', or 'grib' directories) - they will only be discovered by the ooici-iosp codebase if you either:  

>>a) copy the libraries to the ooici/.settings/override-repo by running: "ant stage_netcdf_deps" from the THREDDS directory  

>>>*Note that if you use this method, you will need to run "ant unstage_netcdf_deps" in order to switch back to getting the core dependencies from the remote server*  

>>b) copy the new core libraries to the root level of the ooici.net release server so they are available for the ooici-iosp codebase  

>>>*After the new libraries are available on the server, run "ant resolve" from the THREDDS/ooici/ directory to update the local workspace*  

>1. ant -f cdm/build.xml ooiciMakeUI  

##Compile/release toolsUI jar
Resulting jar file in lib/releases  

    ant relToolsUI


##Compile/release thredds war
Resulting war file in lib/releases  

    ant relTds