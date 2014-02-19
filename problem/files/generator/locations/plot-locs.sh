#!/bin/bash

create_plot(){
# example input "locs-10.0-0.15-0.05.points"

IFS='-' read -ra ADDR <<< "$1"
IFS='.' read -ra EXT <<< "$1"

inputfile=$1 # complete path
outputfile=${EXT[0]}.${EXT[1]}.${EXT[2]}.${EXT[3]}  # name without extension
envSize=${ADDR[1]} # size of environment in km
relativeStd=${ADDR[2]} # standard deviation relative to envSize
binSize=${ADDR[3]}

gnuplot << EOF

set terminal postscript enh eps color
set title 'Locations generated with settings: envSize=$envSize, relativeStd=$relativeStd, binSize=$binSize'
set output '$outputfile.eps'

set key off

set xlabel 'x (km)'
set ylabel 'y (km)'

set size square


set xrange [ 0 : $envSize ] noreverse nowriteback
set yrange [ 0 : $envSize ] noreverse nowriteback



plot '$inputfile'

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



for f in *.points
do
	echo $f
	create_plot $f
done


