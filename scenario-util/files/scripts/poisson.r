times <- read.table("~/workspace/RinSim/problem/files/test/times.txt")
sine <- read.table("~/workspace/RinSim/problem/files/test/sine.txt")

p1 <- ggplot(times,aes(x=V1,y=V2)) + geom_bar(stat="identity")
p2 <- ggplot(sine,aes(x=V1,y=V2)) + geom_point()

show(p1)