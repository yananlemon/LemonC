package site.ilemon.list;

/**
 * <p>DoublyLinkedList</p>  
 * <p>保存任何数据的双向链表 </p>  
 * @author yanan  
 * @date 2018年3月15日
 */
public class DoublyLinkedList<T> {
    private DNode<T> head,tail;
	private int size;
	public DoublyLinkedList(){
		head=tail=new DNode<T>(null,null, null);
	}
	
	public void clear(){
		head = tail = new DNode<T>(null,null, null);
		size = 0;
	}
	
	public int size(){
		return size;
	}
	
	public boolean isEmpty(){
		return size==0?true:false;
	}
	/**
	 * 将数据添加到链表头部
	 * @param data 待添加的数据
	 */
	public void addToHead(T data){
		if(isEmpty()){
			head=tail=new DNode<T>(data, null, null);
		}else{
			DNode<T> newNode = new DNode<T>(data, null, null);
			newNode.next=head;
			head.prev=newNode;
			head=newNode;
		}
		size++;
	}
	
	/**
	 * 将数据添加到链表尾部
	 * @param data 待添加的数据
	 */
	public void addToTail(T data){
		if(isEmpty()){
			head=tail=new DNode<T>(data, null, null);
		}else{
			DNode<T> newNode = new DNode<T>(data, null, null);
			tail.next=newNode;
			newNode.prev=tail;
			tail=tail.next;
		}
		size++;
	}
	
	/**
	 * 从链表头部删除数据并返回改数据，如果链表为空，则返回null。
	 */
	public T deleteFromHead(){
		if(!isEmpty()){
			T value=head.info;
			if(size==1){
				head=tail=new DNode<T>(null, null, null);
			}else{
				head=head.next;
				head.prev=null;
			}
			size--;
			return value;
		}
		return null;
	}
	
	/**
	 * 将数据添加到链表正中间(3.10习题第16题)
	 * @param data 待添加的数据
	 */
	public void addToMiddle(T data){
		DNode<T> newNode = new DNode<T>(data, null, null);
		if(isEmpty()){
			head=tail=newNode;
		}else{
			int index=(size()>>1)-1;
			DNode<T> temp=head;
			while(index>0){
				temp=temp.next;
				index--;
			}
			newNode.next=temp.next;
			temp.next.prev=newNode;
			temp.next=newNode;
			newNode.prev=temp;
		}
		size++;
	}
	
	/**
	 * 从链表尾部删除数据并返回改数据，如果链表为空，则返回null。
	 */
	public T deleteFromTail(){
		if(!isEmpty()){
			T value=head.info;
			if(size==1){
				head=tail=new DNode<T>(null, null, null);
			}else{
				tail=tail.prev;
				tail.next=null;
			}
			size--;
			return value;
		}
		return null;
	}
	
	/**
	 * 从指定位置删除链表中的数据
	 * @param index 数据位于链表中的索引
	 */
	public void deleteNode(int index){
		rangeCheck(index);
		if(index==0){
			deleteFromHead();
		}else if(index==size-1){
			deleteFromTail();
		}else{
			int i=0;
			DNode<T> tmpNode=head;
			while(i<index){
				tmpNode=tmpNode.next;
				i++;
			}
			tmpNode.prev.next=tmpNode.next;
			tmpNode.next.prev=tmpNode.prev;
			size--;
		}
		
	}
	
	
	/**
	 * 获取链表指定位置的数据
	 * @param i 索引
	 * @return int 数据
	 */
	public T get(int i){
		rangeCheck(i);
		if(i==0){
			return head.info;
		}
		if(i==size-1){
			return tail.info;
		}
		int tmpIndex=0;
		DNode<T> tmpNode=head; 
		while(tmpIndex!=i){
			tmpNode=tmpNode.next;
			tmpIndex++;
		}
		return tmpNode.info;
	}
	
	private void rangeCheck(int i) {
		if(i<0 || i>=size){
			throw new ArrayIndexOutOfBoundsException(i);
		}
	}
}

class DNode<T>{
	T info;
	DNode<T> prev,next;
	public DNode(T info, DNode<T> prev, DNode<T> next) {
		super();
		this.info = info;
		this.prev = prev;
		this.next = next;
	}
	
}