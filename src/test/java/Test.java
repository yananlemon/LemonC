public class Test {

	public static void main(String[] args) {
		int a;
		if( 19 > 27 || !(1 > 0 && 1 > 9 || 2 > 1) ){
			a = 1;
		}else{
			a = 0;
		}
		System.out.println(a);

		if( 19 > 27 || !(1 > 0 && 1 > 9 && 2 > 1) ){
			a = 1;
		}else{
			a = 0;
		}
		System.out.println(a);

	}

}
