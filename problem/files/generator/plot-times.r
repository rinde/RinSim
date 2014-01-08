library(ggplot2)

plotArrivalTimes <- function(file){  
  str(file)
  #filepath<-"/Users/rindevanlon/Desktop/test.txt"
  myData<-read.table(file=file,quote="")  
  #str(myData)
  
  c <- ggplot(myData,aes(V1,width=1000), main="Event arrival times")
  c + geom_bar(fill="red",binwidth=1)  + labs(title = "Event arrival times") + labs(x="time") + xlim(0,1000) + ylim(0,1) + stat_ecdf()
  
  print("done")
  ggsave(paste(file,".pdf",sep=""),height=3,width=15) 
}

files <- list.files(path=".",pattern="*\\.times$",recursive=T)
#str(files)
for( f in files){
  plotArrivalTimes(f)
  #break
}