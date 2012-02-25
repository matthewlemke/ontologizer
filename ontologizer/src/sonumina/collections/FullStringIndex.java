package sonumina.collections;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A class mapping strings to other objects.
 * 
 * @author Sebastian Bauer
 */
public class FullStringIndex<T>
{
	private ArrayList<String> stringList = new ArrayList<String>();
	private ArrayList<T> objectList = new ArrayList<T>();

	private class StringIterator implements Iterator<T>
	{
		private String str;
		private int pos = -1;
		
		public StringIterator(String str)
		{
			this.str = str;
		}

		@Override
		public boolean hasNext() {
			while (true)
			{
				pos++;
				if (pos >= stringList.size())
					return false;
				if (stringList.get(pos).contains(str))
					return true;
			}
		}

		@Override
		public T next()
		{
			return objectList.get(pos);
		}

		@Override
		public void remove() { }
	}
	
	/**
	 * Associates the given string with the given object.
	 * 
	 * @param string
	 * @param t
	 */
	public void add(String string, T o)
	{
		stringList.add(string);
		objectList.add(o);
	}
	
	/**
	 * Returns the size of the index, i.e., the total
	 * number of strings.
	 * 
	 * @return
	 */
	public int size()
	{
		return stringList.size();
	}
	
	/**
	 * Returns an iterable which can be used to iterate over 
	 * elements that contain the given string.
	 * 
	 * @param string
	 * @return
	 */
	public Iterable<T> contains(final String string)
	{
		return new Iterable<T>()
				{
					@Override
					public Iterator<T> iterator()
					{
						return new StringIterator(string);
					}
				};
	}
}
