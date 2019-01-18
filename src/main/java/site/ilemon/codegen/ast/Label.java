package site.ilemon.codegen.ast;


public class Label{
    private int i;
    private static int count = 0;

    public Label()
    {
        i = count++;
    }
    
    public Label(int c)
    {
        i = count;
    }

    @Override
    public String toString()
    {
        return "Label_" + i;
    }
    
    public static Label getCurrLable(){
    	return new Label(0);
    }
}
