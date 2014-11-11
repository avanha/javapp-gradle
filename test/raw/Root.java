public class Root {
	public void run() {
#ifdef RELEASE
		validate();
#else
		//validate();
#endif
	}
#ifdef TEST
	public void test() {
	}
#endif
}
