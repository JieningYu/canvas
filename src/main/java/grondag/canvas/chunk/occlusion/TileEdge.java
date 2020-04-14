package grondag.canvas.chunk.occlusion;

abstract class TileEdge {
	protected final int ordinal;


	protected final AbstractTile tile;
	private final int spanSize;
	private final int stepSize;

	// all coordinates are full precision and corner-oriented unless otherwise noted
	private int stepA;
	private int stepB;
	private int spanA;
	private int spanB;
	private int extent;

	private int x0y0;
	private int position = POSITION_DIRTY;

	private int save_x0y0;
	private int save_position;

	public final int ordinalFlag;

	protected TileEdge(int ordinal, AbstractTile tile) {
		ordinalFlag = 1 << ordinal;
		final int diameter = tile.diameter;
		stepSize = diameter / 8;
		spanSize = diameter - 1;
		this.ordinal = ordinal;
		this.tile = tile;
	}

	abstract int a();
	abstract int b();
	abstract int c();
	abstract EdgePosition pos();

	public int compute(int x, int y) {
		return c() + a() * x + b() * y;
	}

	public void push() {
		save_x0y0 = x0y0;
		save_position = position;
	}

	public void pop() {
		x0y0 = save_x0y0;
		position = save_position;
	}

	/**
	 *
	 * Edge functions are line equations: ax + by + c = 0 where c is the origin value
	 * a and b are normal to the line/edge.
	 *
	 * Distance from point to line is given by (ax + by + c) / magnitude
	 * where magnitude is sqrt(a^2 + b^2).
	 *
	 * A tile is fully outside the edge if signed distance less than -extent, where
	 * extent is the 7x7 diagonal vector projected onto the edge normal.
	 *
	 * The length of the extent is given by  (|a| + |b|) * 7 / magnitude.
	 *
	 * Given that magnitude is a common denominator of both the signed distance and the extent
	 * we can avoid computing square root and compare the weight directly with the un-normalized  extent.
	 *
	 * In summary,  if extent e = (|a| + |b|) * 7 and w = ax + by + c then
	 *    when w < -e  tile is fully outside edge
	 *    when w >= 0 tile is fully inside edge (or touching)
	 *    else (-e <= w < 0) tile is intersection (at least one pixel is covered.
	 *
	 * For background, see Real Time Rendering, 4th Ed.  Sec 23.1 on Rasterization, esp. Figure 23.3
	 */
	public void prepare() {
		final int a = a();
		final int b = b();
		stepA = a * stepSize;
		stepB = b * stepSize;
		spanA = a * spanSize;
		spanB = b * spanSize;
		extent = -Math.abs(spanA) - Math.abs(spanB);
	}

	public void moveRight() {
		if (position == POSITION_DIRTY)  {
			return;
		}

		final EdgePosition edgePos = pos();
		x0y0 += a() + spanA;

		if (edgePos.isRight) {
			if (position != OUTSIDE) {
				position  =  POSITION_RECLASSIFY;
			}
		} else if (edgePos.isLeft && position != INSIDE) {
			position = POSITION_RECLASSIFY;
		}
	}

	public void moveLeft() {
		if (position == POSITION_DIRTY)  {
			return;
		}

		final EdgePosition edgePos = pos();
		x0y0 -= (a() + spanA);

		if (edgePos.isLeft) {
			if (position != OUTSIDE) {
				position = POSITION_RECLASSIFY;
			}
		} else if (edgePos.isRight && position != INSIDE) {
			position = POSITION_RECLASSIFY;
		}
	}

	public void moveUp() {
		if (position == POSITION_DIRTY)  {
			return;
		}

		final EdgePosition edgePos = pos();
		x0y0 += b() + spanB;

		if (edgePos.isTop) {
			if (position != OUTSIDE) {
				position = POSITION_RECLASSIFY;
			}
		} else if (edgePos.isBottom && position != INSIDE) {
			position = POSITION_RECLASSIFY;
		}
	}

	public void updateFromParent(TileEdge parent) {
		if (parent.position == POSITION_DIRTY) {
			position = POSITION_DIRTY;
		} else {
			x0y0 = parent.x0y0;
			position = POSITION_RECLASSIFY;
		}
	}

	private int chooseEdgeValue() {
		switch  (pos()) {
		case TOP:
		case TOP_LEFT:
			//			return x0y1;
			return x0y0 + spanB;

		case LEFT:
		case BOTTOM_LEFT:
		case BOTTOM:
			return x0y0;

		case TOP_RIGHT:
			//			return x1y1;
			return x0y0 + spanA + spanB;

		case RIGHT:
		case BOTTOM_RIGHT:
			//			return x1y0;
			return x0y0 + spanA;

		default:
			assert false : "Edge position invalid.";
		return -1;
		}
	}

	private void classify()  {
		final int w = chooseEdgeValue();
		//		cornerValue = w;
		//NB extent is always negative

		if (w < extent) {
			// fully outside edge
			position = OUTSIDE;
		} else if (w >= 0) {
			// fully inside or touching edge
			position = INSIDE;
		} else {
			// intersecting - at least one pixel is set
			position = INTERSECTING;
		}
	}

	public int position() {
		if (position < 0) {
			if (position == POSITION_DIRTY) {
				x0y0 = compute(tile.x(), tile.y());
			}

			classify();
		}

		return position;
	}

	protected long buildMask() {
		final int a = stepA;
		final int b = stepB;

		switch  (pos()) {
		case TOP: {
			int wy = x0y0; // bottom left will always be inside
			assert wy >= 0;
			assert b < 0;

			long yMask = 0xFFL;
			long mask = 0;

			while (wy >= 0 && yMask != 0L) {
				mask |= yMask;
				yMask <<= 8;
				wy += b; //NB: b will be negative
			}

			return mask;
		}

		case BOTTOM: {
			int wy = x0y0 + spanB; // top left will always be inside
			assert wy >= 0;
			assert b > 0;

			long yMask = 0xFF00000000000000L;
			long mask = 0;

			while (wy >= 0 && yMask != 0L) {
				mask |= yMask;
				yMask = (yMask >>> 8); // parens are to help eclipse auto-formatting
				wy -= b;
			}

			return mask;
		}

		case RIGHT: {
			final int wy = x0y0; // bottom left will always be inside
			assert wy >= 0;
			assert a < 0;

			final int x = 7 - Math.min(7, -wy / a);
			long mask = (0xFF >> x);

			mask |= mask << 8;
			mask |= mask << 16;
			mask |= mask << 32;

			return mask;
		}

		case LEFT: {
			final int wy = x0y0 + spanA; // bottom right will always be inside
			assert wy >= 0;
			assert a > 0;

			final int x =  7 - Math.min(7, wy / a);
			long mask = (0xFF << x) & 0xFF;

			mask |= mask << 8;
			mask |= mask << 16;
			mask |= mask << 32;

			return mask;
		}

		case TOP_LEFT: {
			// PERF: optimize case when shallow slope and several bottom rows are full

			int wy = x0y0 + spanA; // bottom right will always be inside
			assert wy >= 0;
			assert b < 0;
			assert a > 0;

			// min y will occur at x = 0;

			long mask = 0;
			int yShift = 0;

			while (yShift < 64 && wy >= 0) {
				// x  here is first not last
				final int x =  7 - Math.min(7, wy / a);
				final int yMask = (0xFF << x) & 0xFF;
				mask |= ((long) yMask) << yShift;
				wy += b; //NB: b will be negative
				yShift += 8;
			}

			return mask;
		}

		case BOTTOM_LEFT: {
			int wy = x0y0 + spanA + spanB; // top right will always be inside
			assert wy >= 0;
			assert b > 0;
			assert a > 0;

			// min y will occur at x = 7;

			int yShift = 8 * 7;
			long mask = 0;

			while (yShift >= 0 && wy >= 0) {
				// x  here is first not last
				final int x =  7 - Math.min(7, wy / a);
				final int yMask = (0xFF << x) & 0xFF;
				mask |= ((long) yMask) << yShift;
				wy -= b;
				yShift -= 8;
			}

			return mask;
		}

		case TOP_RIGHT: {
			// PERF: optimize case when shallow slope and several bottom rows are full

			// max y will occur at x = 0
			// Find highest y index of pixels filled at given x.
			// All pixels with lower y value will also be filled in given x.
			// ax + by + c = 0 so y at intersection will be y = -(ax + c) / b
			// Exploit step-wise nature of a/b here to avoid computing the first term
			// logic in other cases is similar
			int wy = x0y0; // bottom left will always be inside
			assert wy >= 0;
			assert b < 0;
			assert a < 0;

			long mask = 0;
			int yShift = 0;

			while(yShift < 64 && wy >= 0) {
				final int x =  7  - Math.min(7, -wy / a);
				final int yMask = (0xFF >> x);
				mask |= ((long) yMask) << yShift;
				wy += b;
				yShift +=  8;
			}

			return mask;
		}

		case BOTTOM_RIGHT: {
			// PERF: optimize case when shallow slope and several top rows are full

			int wy = x0y0 + spanB; // top left will always be inside
			assert wy >= 0;
			assert b > 0;
			assert a < 0;

			int yShift = 8 * 7;
			long mask = 0;

			while (yShift >= 0 && wy >= 0) {
				final int x = 7 - Math.min(7, -wy / a);
				final int yMask = (0xFF >> x);
				mask |= ((long) yMask) << yShift;
				wy -= b;
				yShift -= 8;
			}

			return mask;
		}

		default:
			assert false : "Edge flag out of bounds.";
		return 0L;
		}
	}

	static final int OUTSIDE = 1;
	static final int INTERSECTING = 2;
	static final int INSIDE = 4;
	static final int POSITION_DIRTY = -1;
	static final int POSITION_RECLASSIFY = -2;

	public void makeDirty() {
		position = POSITION_DIRTY;
	}
}