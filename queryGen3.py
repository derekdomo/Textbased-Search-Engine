#!/usr/bin/env python
# coding=utf-8
forigin = open("queriesAnd.txt")
fd = open("qry.txt", 'w+')
lines = forigin.readlines()
result = []
w=["0.8",'0.2']
weight = ['0.10', '0.45', '0.45']
for l in lines:
    temp = l.split(":")
    term = temp[1].split(" ")
    line = "#WAND( "+w[0]+" #WAND("
    termii = weight[0] + " #And("
    term[-1] = term[-1].strip()
    for i in term:
        termii = termii + " " + i
    line = temp[0] + ":" + line + termii + ") "
    termii = weight[1]+" #AND(" 
    for i in range(len(term)-1):
        termii = termii + " #NEAR/1( " + term[i]+" "+term[i+1]+")"
    line = line + termii + ") "
    termii = weight[2] + " #AND("
    for i in range(len(term)-1):
        termii = termii + " #window/8(" + term[i] + " " + term[i+1]+")"  
    line = line + termii + ")) "
    termii=" "+w[1]+" #AND("
    for i in term:
        tt=" #WSUM(0.05 "+i+".url 0.05 "+i+".keywords 0.05 "+i+".title 0.8 "+i+".body 0.05 "+i+".inlink)"
        termii=termii+tt
    line=line+termii+")"
    result.append(line.strip() + ")\n")
fd.writelines(result)
