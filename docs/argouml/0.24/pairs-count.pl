#!/usr/bin/perl
# The published architecture measured on module pairs rather than on one version's violations: of the ordered pairs
# of modules (present in 0.24) that the architecture forbids, how many the layering alone forbids (the source is in
# a slower layer than the target), how many an apart decision within each layer forbids, and how many only a
# written clause can state (the source is in a faster layer than the target, but the relation is not allowed).
# Usage: perl pairs-count.pl argouml.sysmdl packages.txt
use strict; use warnings;
my ($sysmdl, $packages) = @ARGV;
open my $h, '<', $sysmdl or die; my @lines = <$h>; close $h; chomp @lines;
my (%rx, %allowed, @modules, $section);
for my $l (@lines) {
    $l =~ s/\r$//;
    if ($l =~ /^#\s*(\w+)/) { $section = $1; next; }
    next if $l =~ /^\s*(#|$)/;
    if ($section eq 'modules') { push @modules, $l; }
    elsif ($section eq 'mapping') { my ($m, $r) = split /\s+/, $l, 2; $rx{$m} = $r; }
    elsif ($section eq 'relations') { my ($a, $b) = split /\s+/, $l; $allowed{$a}{$b} = 1; }
}
open my $p, '<', $packages or die; my @pk = <$p>; close $p; chomp @pk;
my %members;
for my $pkg (@pk) {
    $pkg =~ s/\r$//;
    for my $m (@modules) {
        next unless exists $rx{$m};
        my $r = $rx{$m};
        if ($pkg =~ /^(?:$r)$/ || $pkg =~ /^(?:$r)/) { push @{$members{$m}}, $pkg; last; }
    }
}
my %depth;
sub depth { my $m = shift; return $depth{$m} if exists $depth{$m}; my $d = 0; for my $t (keys %{$allowed{$m} || {}}) { my $x = depth($t) + 1; $d = $x if $x > $d; } $depth{$m} = $d; return $d; }
depth($_) for @modules;
my @present = grep { @{$members{$_} || []} } @modules;
my ($pairs, $allowedPairs, $order, $apart, $written) = (0, 0, 0, 0, 0);
my @writtenPairs;
for my $a (@present) {
    for my $b (@present) {
        next if $a eq $b;
        $pairs++;
        if ($allowed{$a}{$b}) { $allowedPairs++; next; }
        if ($depth{$a} < $depth{$b}) { $order++; }
        elsif ($depth{$a} == $depth{$b}) { $apart++; }
        else { $written++; push @writtenPairs, "$a -> $b"; }
    }
}
my $forbidden = $pairs - $allowedPairs;
printf "modules present %d, ordered pairs %d, allowed %d, forbidden %d\n", scalar @present, $pairs, $allowedPairs, $forbidden;
printf "forbidden by the order alone %d (%.0f%%), by apart within a layer %d (%.0f%%), only by a written clause %d (%.0f%%)\n",
    $order, 100 * $order / $forbidden, $apart, 100 * $apart / $forbidden, $written, 100 * $written / $forbidden;
print "written: $_\n" for @writtenPairs;
