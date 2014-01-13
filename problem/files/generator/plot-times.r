library(ggplot2)
library(grid)
library(gridExtra)
library(tikzDevice)
library(foreach)
library(doMC)
registerDoMC(24)


plotArrivalTimes <- function(file){  
  str(file)
  #filepath<-"/Users/rindevanlon/Desktop/test.txt"
  myData<-read.table(file=file,quote="")  
  #str(myData)
  
  c <- ggplot(myData,aes(V1,width=1000)) + geom_bar(fill="red",binwidth=1)  + labs(x="time", y="event count") + xlim(0,1000)+ theme_bw()
  
  e <- ggplot(myData,aes(V1,width=1000), main="Event arrival times")  + geom_abline(slope=1/1000, color="grey", linetype=1,size=1) + xlim(0,1000) + theme_bw()+ stat_ecdf()  + labs(x="time", y="perc. of known events")

  # can be tikz or pdf
  pdf(paste(file,".pdf",sep=""),height=6,width=15)    
  grid.arrange(c, e, ncol = 1, main = "Event arrival times")
  dev.off()    
}

files <- list.files(path=".",pattern="*\\.times$",recursive=T)
#str(files)
foreach( i=1:length(files)) %dopar%{
  plotArrivalTimes(files[i])
 # break
}