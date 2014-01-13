library(ggplot2)
library(grid)
library(gridExtra)
library(tikzDevice)
library(foreach)
library(doMC)
registerDoMC(detectCores())

str(paste("Detected",detectCores(), "cores."))

plotArrivalTimes <- function(file){  
  str(file)
  #filepath<-"/Users/rindevanlon/Desktop/test.txt"
  myData<-read.table(file=file,quote="")  
  
  len <- myData[1,]
  times <- data.frame(V1=myData[-1,])
  
  .e <- environment()
  c <- ggplot(times, aes(V1,width=len),environment = .e) + geom_bar(fill="red",binwidth=1)  + labs(x="time", y="event count") + xlim(0,len)+ theme_bw()
  
  e <- ggplot(times,aes(V1,width=len),environment = .e, main="Event arrival times")  + geom_abline(slope=1/len, color="grey", linetype=1,size=1) + xlim(0,len) + theme_bw()+ stat_ecdf()  + labs(x="time", y="perc. of known events")

  # can be tikz or pdf
  pdf(paste(file,".pdf",sep=""),height=6,width=15)    
  grid.arrange(c, e, ncol = 1, main = "Event arrival times")
  dev.off()    
}

files <- list.files(path=".",pattern="*\\.times$",recursive=T)


#plotArrivalTimes(files[1])

foreach( i=1:length(files)) %dopar%{
  plotArrivalTimes(files[i])
}