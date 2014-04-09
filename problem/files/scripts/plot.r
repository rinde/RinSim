library(ggplot2)
library(foreach)
library(doMC)
registerDoMC(detectCores())

plotIntensityFunction <-  function(file){  
  myData<-read.table(file=file,quote="")  
  p <- ggplot(myData, aes(x=V1,y=V2)) + geom_line() + ylim(0,.052)
  ggsave(paste(file,".pdf",sep=""),plot=p)
}

dir <- "~/workspace/RinSim/problem/files/test/times/sine/"

files <- list.files(path=dir,pattern="*\\.intens$",recursive=T,full.names=T)

foreach( i=1:length(files)) %dopar%{
  plotIntensityFunction(files[i])
}

pdfs <- list.files(path=dir,pattern="*\\.intens.pdf$",recursive=T)
str(paste(pdfs,collapse=" "))
str("merge pdfs")
cmdStr <- paste("cd ",dir,"; pdftk",paste(pdfs,collapse=" "),"cat output all.pdf", sep=" ")
system(cmdStr)