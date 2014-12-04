#!/bin/bash

create_plot(){
# example input "hist-10.0-0.15-0.05.data"

IFS='-' read -ra ADDR <<< "$1"
IFS='.' read -ra EXT <<< "$1"

inputfile=$1 # complete path
outputfile=${EXT[0]}.${EXT[1]}.${EXT[2]}.${EXT[3]}  # name without extension
envSize=${ADDR[1]} # size of environment in km
relativeStd=${ADDR[2]} # standard deviation relative to envSize
binSize=${ADDR[3]}

gnuplot << EOF

set terminal postscript enh eps color
set title 'Comparison of normal distribution with discrete approximation. envSize=$envSize, relativeStd=$relativeStd, binSize=$binSize'
set output '$outputfile.eps'

set xlabel 'size in km'

normal(x,mu,sigma)=sigma<=0?1/0:invsqrt2pi/sigma*exp(-0.5*((x-mu)/sigma)**2)
invsqrt2pi = 0.398942280401433

xmin = -.5
xmax = $envSize+.5
set xrange [ xmin : xmax ] noreverse nowriteback

mu=$envSize/2
sigma=$envSize*$relativeStd

normlabel=sprintf("N(%d,%0.1f)",mu,sigma)

plot '$inputfile' with steps title 'discrete approximation', normal(x,mu,sigma) title normlabel

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



for f in *.data
do
	echo $f
	create_plot $f
done


