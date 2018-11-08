
public class TestMain {

	/*public static void main(String[] args) {
		int start;
		int end;
		int result;
		start = 1;
		end = 100;
		result = sum(start,end);
		System.out.printf("%d加到%d的值是%d",start,end,result);
	}
	
	static int sum(int a,int b){
		return (a+b)*(b-a+1)/2;
	}*/
	
	public static void main(String[] args) {
		int t;
		int n;
		n = 12;
		t = fib(n);
		System.out.println(t);
	}	
	/*// 打印九九乘法表
	static int showMulTable(){
		int k;
		k = 1;
		while(k < 10 ){
			System.out.printf("%d\n",k);
			k = k+1;
		}
		return 0;
	}*/

	static int fib(int n){
		int rs;
		if( n < 3 ){
			rs = 1;
		}else{
			rs = fib(n-1) + fib(n-2);
		}
		return rs;
	}

}
