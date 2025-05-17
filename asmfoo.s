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
mov $-2147483648, %r13d
mov $-1, %r12d
mov %r13d, %eax
cltq
cqto
idiv %r12d
mov %eax, %r12d
mov %r12d, %eax
cltq
ret
