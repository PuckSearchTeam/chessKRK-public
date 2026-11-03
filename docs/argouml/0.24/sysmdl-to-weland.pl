#!/usr/bin/perl
# From the published architecture of ArgoUML 0.24 (Brunet et al. 2012, as distributed in s4rdm3x's argouml.sysmdl:
# modules, a mapping by regular expressions on package names, and the allowed relations between modules) and the
# list of the subject's packages, two Weland contexts:
#   exact.wld   the published architecture as a reflexion model: one hide clause per module, listing every module
#               it is not allowed to use (settled module by module);
#   ordered.wld the layering the allowed relations imply (longest path in the allowed DAG), as a speed chain, with
#               nothing else: what an order alone says.
# Also prints the packages no module maps (the orphans of the published mapping) and the layers.
use strict; use warnings;
my ($sysmdl, $packages, $exact, $ordered) = @ARGV;
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
my (%members, @orphans);
for my $pkg (@pk) {
    $pkg =~ s/\r$//;
    my $found;
    for my $m (@modules) {
        next unless exists $rx{$m};
        my $r = $rx{$m};
        if ($pkg =~ /^(?:$r)$/ || $pkg =~ /^(?:$r)/) { push @{$members{$m}}, $pkg; $found = 1; last; }
    }
    push @orphans, $pkg unless $found;
}
# Layers: longest path in the allowed-relations DAG (a uses b): depth(a) = 1 + max depth(b) over allowed targets.
my %depth;
sub depth { my $m = shift; return $depth{$m} if exists $depth{$m}; my $d = 0; for my $t (keys %{$allowed{$m} || {}}) { my $x = depth($t) + 1; $d = $x if $x > $d; } $depth{$m} = $d; return $d; }
depth($_) for @modules;
my %present = map { ($_ => 1) } grep { @{$members{$_} || []} } @modules;
print STDERR "orphan packages (no module of the published mapping): @orphans\n";
print STDERR "modules without packages in this version: ", join(", ", grep { !$present{$_} } @modules), "\n";

sub axis_lines {
    my $out = '';
    for my $m (@modules) { next unless $present{$m}; $out .= sprintf("axis %-20s = [%s]\n", $m, join(", ", @{$members{$m}})); }
    return $out;
}
my $head = "// ArgoUML 0.24, the published architecture of Brunet et al. (2012), as distributed with s4rdm3x (argouml.sysmdl):\n"
         . "// 19 modules mapped to packages by regular expression, and the relations each module is allowed to have.\n"
         . "// Packages no module maps are outside the context: " . join(", ", @orphans) . ".\n";
open my $e, '>', $exact or die; print $e $head, "// This file states the published architecture exactly, as a reflexion model does: one clause per module,\n// listing every module it may not use.\n", axis_lines();
for my $m (@modules) {
    next unless $present{$m};
    my @forbidden = grep { $present{$_} && $_ ne $m && !$allowed{$m}{$_} } @modules;
    print $e "hide ", join(", ", @forbidden), " from $m;\n" if @forbidden;
}
print $e "friend tests of all;\n"; close $e;
open my $o, '>', $ordered or die; print $o $head, "// This file states only the layering the allowed relations imply: each module at the depth of its longest chain\n// of allowed uses, the layers as a speed chain, slowest first. What an order alone says.\n", axis_lines();
my %bylayer; push @{$bylayer{$depth{$_}}}, $_ for grep { $present{$_} } @modules;
my @chain = map { join(", ", @{$bylayer{$_}}) } sort { $a <=> $b } keys %bylayer;
print $o "speed ", join(" < ", @chain), ";\nfriend tests of all;\n"; close $o;
print STDERR "layers (slowest first): ", join(" < ", @chain), "\n";
