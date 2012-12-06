package net.sashag.wams.android;

import java.util.List;

public interface MobileServiceCallbackWithResults<E> {
	void completedSuccessfully(List<E> results);
	void errorOccurred(MobileException exception);
}
