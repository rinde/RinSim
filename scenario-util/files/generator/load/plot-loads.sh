#!/bin/bash

create_plot(){
# example input "scenario1.load"

IFS='-' read -ra ADDR <<< "$1"
IFS='.' read -ra EXT <<< "$1"

inputfile=$1 # complete path
outputfile=${EXT[0]}  # name without extension
#envSize=${ADDR[1]} # size of environment in km
#relativeStd=${ADDR[2]} # standard deviation relative to envSize
#binSize=${ADDR[3]}

gnuplot << EOF

set terminal postscript enh eps color
set title 'Load graph'
set output '$outputfile.eps'

set xlabel 'time (minutes)'

#normal(x,mu,sigma)=sigma<=0?1/0:invsqrt2pi/sigma*exp(-0.5*((x-mu)/sigma)**2)
#invsqrt2pi = 0.398942280401433

#xmin = -.5
#xmax = $envSize+.5
#set xrange [ xmin : xmax ] noreverse nowriteback

#mu=$envSize/2
#sigma=$envSize*$relativeStd

#normlabel=sprintf("N(%d,%0.1f)",mu,sigma)

plot '$inputfile' with steps title 'load'

EOF

temp="-tmp.pdf"
eps=".eps"
pdf=".pdf"
ps2pdf -dEmbedAllFonts=true -dEPSCrop $outputfile$eps $outputfile$temp

gs -q -dNOPAUSE -dBATCH -sDEVICE=pdfwrite -sOutputFile=$outputfile$pdf $outputfile$temp

rm $outputfile$eps
rm $outputfile$temp
#open $outputfile$pdf
}



for f in *.load
do
	echo $f
	create_plot $f
done


