package de.matdue.isk;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.View;

public class HeightSetter implements AnimatorUpdateListener {
	
	private View animatedView;
	
	public HeightSetter(View view) {
		animatedView = view;
	}

	@Override
	public void onAnimationUpdate(ValueAnimator animation) {
		animatedView.getLayoutParams().height = (Integer) animation.getAnimatedValue();
		animatedView.requestLayout();
	}

}
