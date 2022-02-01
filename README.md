# linuxcnc_probescripts

Collection of scripts for several touch-probe routines.

Also, there is a nice post processor for handwritten linuxcnc ngc files:
It automatically relabels all o-labels (i.e o123) of o-codes like if and while.
You even can write your code without any labels, and the processor does everything!

The scrips can be found at `./subroutines`. They can be configured by editing `_pinit.ngc`. Here you can set feed rates and so on.

All scripts which don't start with an underscore `_` are meant to be called directly from your subroutines or the MDI.