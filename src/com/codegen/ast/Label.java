package com.codegen.ast;


public class Label{
    private int i;
    private static int count = 0;

    public Label()
    {
        i = count++;
    }

    @Override
    public String toString()
    {
        return "Label_" + i;
    }
}
