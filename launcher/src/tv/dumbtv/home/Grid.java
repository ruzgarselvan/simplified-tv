package tv.dumbtv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

/**
 * Lays its children out {@code cols} to a row, left to right then down, at fixed positions: no
 * sideways scrolling, so child {@code i} always sits in column {@code i % cols}, row {@code i / cols}.
 * All rows sit together in one glass box, like a dock, drawn behind the children.
 */
final class Grid extends ViewGroup {
    final int cols;
    private final int gapX, gapY, padX, padY;
    private final Ui.Glass box = new Ui.Glass(Ui.Glass.DOCK, Ui.dp(getContext(), 34));
    private int cellW, cellH;

    Grid(Context c, int cols, int gapX, int gapY, int padX, int padY) {
        super(c);
        this.cols = cols;
        this.gapX = gapX;
        this.gapY = gapY;
        this.padX = padX;
        this.padY = padY;
    }

    int rowCount() {
        return (getChildCount() + cols - 1) / cols;
    }

    /** Top edge of the tiles of grid row {@code r}, in this view's coordinates. */
    int rowTop(int r) {
        return getPaddingTop() + padY + r * (cellH + gapY);
    }

    int cellHeight() {
        return cellH;
    }

    /** Space between a row's box and its tiles. */
    int boxPad() {
        return padY;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        cellW = 0;
        cellH = 0;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View c = getChildAt(i);
            c.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            cellW = Math.max(cellW, c.getMeasuredWidth());
            cellH = Math.max(cellH, c.getMeasuredHeight());
        }
        int rows = rowCount();
        int h = rows == 0 ? 0 : rows * cellH + (rows - 1) * gapY + 2 * padY;
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), h + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            int x = getPaddingLeft() + padX + i % cols * (cellW + gapX);
            int y = rowTop(i / cols);
            c.layout(x, y, x + c.getMeasuredWidth(), y + c.getMeasuredHeight());
        }
        int halo = Ui.dp(getContext(), 10);
        box.setBounds(getPaddingLeft() - halo, getPaddingTop() - halo, r - l - getPaddingRight() + halo, b - t - getPaddingBottom() + halo);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (getChildCount() > 0) box.draw(canvas);
        super.dispatchDraw(canvas);
    }
}
