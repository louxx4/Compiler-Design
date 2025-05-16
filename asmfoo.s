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
mov $-1, %r13
mov $-1, %r12
mov $0, %rdx
mov %r13, %rax
idiv %r12
mov %rax, %r12
mov %r12, %rax
ret
