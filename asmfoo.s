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
mov $5, %r10
mov 1, %cl
shl %r10
mov 5, %r11
mov %cl, 1
shl %r11
imul %r10, %r11
mov 2, %cl
shl %r10
mov %r10, %rax
RET
