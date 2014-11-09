#!/usr/bin/perl

#
#  This perl script illustrates fetching information from a CGI program
#  that typically gets its data via an HTML form using a POST method.
#
#  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
#

use LWP::Simple;

my $fileIn = $ARGV[0];
my $url = 'http://boston.lti.cs.cmu.edu/classes/11-642/HW/HTS/tes.cgi';

#  Fill in your USERNAME and PASSWORD below.

my $ua = LWP::UserAgent->new();
   $ua->credentials("boston.lti.cs.cmu.edu:80", "HTS", "xiangyus", "NzNiOGVj");
my $result = $ua->post($url,
		       Content_Type => 'form-data',
		       Content      => [ logtype => 'Summary',	# cgi parameter
					 infile => [$fileIn],	# cgi parameter
					 hwid => 'HW4'		# cgi parameter
		       ]);

my $result = $result->as_string;	# Reformat the result as a string
   $result =~ s/<br>/\n/g;		# Replace <br> with \n for clarity
my $MAP = substr $result, index($result, 'map'), 27;
my $P10 = substr $result, index($result, 'P10'), 27;
my $P20 = substr $result, index($result, 'P20'), 27;
my $P30 = substr $result, index($result, 'P30'), 27;
print $MAP;
print $P10;
print $P20;
print $P30;

exit;
