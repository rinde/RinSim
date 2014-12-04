library(ggplot2)
library(plyr)
ggplotRegression <- function (data,formula) {
  show(formula)
  fit = lm(formula,data=data)
  show(summary(fit))
  require(ggplot2)
  
  ggplot(data, aes_string(x = names(fit$model)[2], y = names(fit$model)[1])) + 
    geom_point() +
    geom_line() +
    stat_smooth(method = "lm", formula=formula, col = "red") +
    labs(title = paste("Adj R2 = ",signif(summary(fit)$adj.r.squared, 5),
                       "; Intercept =",signif(fit$coef[[1]],5 ),
                       "; Slope =",signif(fit$coef[[2]], 5),
                       "; P =",signif(summary(fit)$coef[2,4], 5)))
}

intensities <- rename(read.table("~/workspace/RinSim/problem/files/test/times/intensity-analysis.txt", quote="\""), c("V1"="x", "V2"="y"))


#intensities <- subset(intensities, x < 1 & x > -.99)

show(intensities)
p <- ggplotRegression(intensities, y ~ x  )
#p <- ggplot(intensities, aes(x=V1,y=V2)) + geom_line()
show(p)

