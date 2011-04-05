header
{
package test;
}

options {
}

class SampleParser extends Parser;

options {
    k = 2;
    exportVocab=Sample;
	buildAST = true;
}

constant
  : (ICON | CHCON)
  ;
