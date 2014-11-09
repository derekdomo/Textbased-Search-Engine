#!/usr/bin/perl

#
#  This perl script illustrates fetching information from a CGI program
#  that typically gets its data via an HTML form using a POST method.
#
#  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
#

use LWP::Simple;
use strict;
use warnings;

my $fileIn = $ARGV[0];
my $url = 'http://boston.lti.cs.cmu.edu/classes/11-642/HW/HTS/tes.cgi';

#  Fill in your USERNAME and PASSWORD below.

my $ua = LWP::UserAgent->new();
   $ua->credentials("boston.lti.cs.cmu.edu:80", "HTS", "xiangyus", "NzNiOGVj");
my $result = $ua->post($url,
		       Content_Type => 'form-data',
		       Content      => [ logtype => 'Detailed',	# cgi parameter
					 infile => [$fileIn],	# cgi parameter
					 hwid => 'HW4'		# cgi parameter
		       ]);

$result = $result->as_string;	# Reformat the result as a string
$result =~ s/<br>/\n/g;		# Replace <br> with \n for clarity
my @basel=("0.0170","0.1168","0.0615","0.0344","0.3836","0.0007","0.0340","0.0432","0.0165","0.2721");
my @lines = split('\n',$result);
my @statsMap = grep{/map/} @lines;
my @statsP10 = grep{/P10\b/} @lines;
my @statsP20 = grep{/P20\b/} @lines;
my @statsP30 = grep{/P30\b/} @lines;
my $filename = "stats.csv";
open(my $fh, '>', $filename) or die "Couldn't open file";
my $i=0;
my $win=0;
my $loss=0;
foreach(@statsMap){
    if ($_ =~ m/all/){

    }else{
        my @sepe = split(/\t/, $_);
        $sepe[0] =~ s/\s//g;
        print $fh "$sepe[0],$sepe[1],$sepe[2]\n";
        if ($basel[$i] gt $sepe[2]) {
            print "$basel[$i] > $sepe[2]\n";
            $loss=$loss+1;
        }
        if ($basel[$i] lt $sepe[2]){
            print "$basel[$i] < $sepe[2]\n";
            $win=$win+1;
        }
        $i=$i+1;
    }
}
print "win/loss:$win/$loss\n";
foreach(@statsP10){
    if ($_ =~ m/all/){
        
    }else{
        my @sepe = split(/\t/, $_);
        $sepe[0] =~ s/\s//g;
        print $fh "$sepe[0],$sepe[1],$sepe[2]\n";
    }
}
foreach(@statsP20){
    if ($_ =~ m/all/){

    }else{
        my @sepe = split(/\t/, $_);
        $sepe[0] =~ s/\s//g;
        print $fh "$sepe[0],$sepe[1],$sepe[2]\n";
    }
}
foreach(@statsP30){
    if ($_ =~ m/all/){

    }else {
        my @sepe = split(/\t/, $_);
        $sepe[0] =~ s/\s//g;
        print $fh "$sepe[0],$sepe[1],$sepe[2]\n";
    }
}
close $fh;
exit;
