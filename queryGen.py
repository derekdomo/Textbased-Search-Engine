#!/usr/bin/env python
# coding=utf-8
forigin=open("queriesAnd.txt")
fd=open("qry.txt",'w+')
lines=forigin.readlines()
result=[]
#weight=['0.10', '0.20', '0.30', '0.20','0.20']
#weight=['0.10', '0.20', '0.20', '0.40','0.10']
#weight=['0.05', '0.10', '0.15', '0.60','0.10']
#weight=['0.05', '0.05', '0.05', '0.80','0.05']
weight=['0', '0.10', '0.05', '0.80','0.05']
for l in lines:
    temp=l.split(":")
    term=temp[1].split(" ")
    line="#AND("
    termii=""
    term[-1]=term[-1].strip()
    for i in term:
        tt=" #WSUM("+weight[0]+" "+i+".url "+weight[1]+" "+i+".keywords "+weight[2]+" "+i+".title"+" "+weight[3]+" "+i+".body "+weight[4]+" "+i+".inlink)"
        termii=termii+tt
    line=temp[0]+":"+line+termii
    result.append(line.strip()+")\n")
fd.writelines(result)
