.global main
.global _main
.text
main:
call _main
# move the return value into the first argument for the syscall
movq %rax, %rdi
# move the exit syscall number into rax
movq $0x3C, %rax
syscall
_main:
mov $7, %r13d
mov $0, %r12d
sub %r13d, %r12d
mov %r12d, %eax
cltq
ret
