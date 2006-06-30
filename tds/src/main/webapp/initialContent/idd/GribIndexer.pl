#!/usr/bin/perl
#
# Name:  	GribIndexer.pl
# 
# Author: 	Robb Kambic
# Date  : 	Oct 12, 2005
# 
# Purpose: 	walks directory sturcture making Grib Indexes as needed.
#    
# Description:  
#
# @param  startdir the directory where to start indexing
# @param  configuration file that has the above parameters on one line.
#
# needed environment
$JAVA = "/opt/jdk1.5/bin/java -Xmx256m";
$ENV{ 'CLASSPATH' } = ":/opt/tomcat/webapps/thredds/WEB-INF/lib/grib.jar:/opt/tomcat/webapps/thredds/WEB-INF/lib/jpeg2000.jar";
#
# process command line switches
while ($_ = $ARGV[0], /^-/) {
	 shift;
       last if /^--$/;
	      /^-D(.*)/ && ($debug = $1);
	     /^(-v)/ && $verbose++;
	     /^(-d)/ && ( $startDir = shift ) ;
	     /^(-f)/ && ( $conf = shift ) ;
}
# configuration file given to process
if( defined( $conf ) ) {
	print "Start ", `/bin/date`;
	open( CONF, "$conf" ) || die "cannot open $conf $!";
	while( <CONF> ) {
		next if( /^#/ );
		chop();
		$startDir = $_ ;
		startIndexing();
	}
	close CONF;
	print "End ", `/bin/date`;
} else {
	print "Start ", `/bin/date`;
	startIndexing();
	print "End ", `/bin/date`;
}

sub startIndexing {

if( ! -d $startDir ) {
	print "$startDir doesn't exist\n";
	return;
}
chdir( $startDir );
# exit dir if multiple indexers running
if( -e "IndexLock" ) {
	print "Exiting directory, another indexing is in ", `pwd`;
	chdir( ".." );
	return;
}
#print "Entering startIndexing, currently in ", `pwd`;
# lock this dir from other indexers
open( LOCK, ">IndexLock" );
close( LOCK );
opendir( TOP, $startDir ) || die "cannot open $startDir $!";
( @TOP ) = readdir( TOP );
closedir( TOP );
checkDirs( @TOP );
# remove index lock
unlink( "IndexLock" );
#print "Leaving startIndexing, currently in ", `pwd`;

} # end startIndexing

# read first couple lines in a file to see if index length = file size
sub indexCheck
{

( $grib, $index ) = @_ ;
local( $size );

open( IN, "$index" );
while( <IN> ) {
	next unless( /length = (\d{1,20})/ );
	$size = $1;
	if( $size != -s $grib ) {
		return 1;
	}
	return 0;
}
} # end indexCheck
#
#
# checkDirs is a recursive routine used to walk the directory structure in a
# depth first search for GRIB files to index. 
#
sub checkDirs{

my ( @INODES ) = @_;
local( $i, @subINODES, $cmd, $gbx, $status );

#print "Entering checkDirs, currently in ", `pwd`;
for( $i = 0; $i <= $#INODES; $i++ ) {
	# skip ., .., and links
	next if( $INODES[ $i ] =~ /^\.$|^\.\.$/ || -l $INODES[ $i ] );
	# grib files end with grib(1|2)
	if( $INODES[ $i ] =~ /grib(\d)$/ ) {
		$version = $1;
		$gbx = $INODES[ $i ] . ".gbx";
		$cmd = "$JAVA ucar/grib/grib" . $version . "/Grib" . $version;
		if( ! -e $gbx ) { # make a new index
			# create new index
			$cmd .= "Indexer"; 
			print "Indexing $INODES[ $i ] ", `/bin/date`;
			$status = `$cmd $INODES[ $i ] $gbx`;

		# only look at files < 3 hours old && need reindex
		} elsif( -M $INODES[ $i ] < 0.1 && indexCheck( $INODES[ $i ], $gbx )) {
			$cmd .= "IndexExtender"; 
			print "IndexExtending $INODES[ $i ] ", `/bin/date`;
			$status = `$cmd $INODES[ $i ] $gbx`;
		}
	# get next set of INODES and step down into directory
	} elsif( -d $INODES[ $i ] ) { 
		opendir( DIR, $INODES[ $i ] ) || 
			die "cannot open $INODES[ $i ] $!";
		( @subINODES ) = readdir( DIR );
		closedir( DIR );
		chdir( $INODES[ $i ] );
		print "currently in ", `pwd`;
		checkDirs( @subINODES );
		chdir( ".." );
		print "currently in ", `pwd`;
	} 
}
#print "Leaving checkDirs, currently in ", `pwd`;
} # end checkDirs
