public class Test {

	public static void main(String[] args) {
		int a = 19;
		int b = 18;
		int c = 5;
		int d = 10;
		if( !( a > b ) && !(c > d) ){
			a = 1;
		}else{
			a = 2;
		}
		System.out.println(a);//2
	}
}
